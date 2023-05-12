package io.github.happysnaker.hbotcore.config;

import com.alibaba.fastjson.JSONObject;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.IOUtil;
import lombok.NonNull;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static io.github.happysnaker.hbotcore.utils.HBotUtil.getContent;

/**
 * HRobot 提供了一套默认的配置管理方式，此类是集中的管理类
 * <p>
 * 在 HRobot 默认逻辑中，工作路径下的 config/config.yaml 将保存用户机器人的默认配置,
 * HRobot 有一套约定来管理配置，遵循这些默认的约定可以使你更专注与开发机器人的核心功能，这些约定包括：
 * <ul>
 *     <li>使用 {@link ConfigManager#setConfigClass(Class)} 方法或使用 {@link HBotConfigComponent} 注解指定你的配置类，配置类中指定你的配置属性，这些属性名必须是静态的，并且与
 *     config.yaml 中的配置项名称一致，HRobot 启动时会自动读取配置文件的配置填充至配置类中</li>
 *     <li>解析时能够支持对象转换，并且支持简单的泛型对象设置，例如 List&lt;User>，但嵌套的泛型对象可能无能为力，嵌套泛型请使用 Map 接收，不鼓励过于复杂的配置项</li>
 *     <li>如果配置文件不存在，此类将会自动创建配置文件，HRobot 允许机器人开发者指定一个模板，可通过 {@link #usingTemplate(URL)} 方法设置模板资源</li>
 *     <li>此类方法 {@link #writeConfig()} 可以将配置类中的配置重写回配置文件，但注释和顺序可能会丢失，默认情况下所有属性都将被写回，可以使用
 *     {@link #addExclude(String...)} 将配置类中的某些配置排除在外</li>
 *     <li>此类暴露方法 {@link #loadConfig()} 加载配置文件到配置类中，程序可以动态的刷新配置，此类提供监听机制使开发者可以监听配置变更，可参考
 *     {@link ConfigListener}，请注意，此类通过 {@link #equals(Object)} 方法比对数据是否变更</li>
 * </ul>
 */
public class ConfigManager {
    /**
     * 用户指定的配置类
     */
    private static Class<?> configClass;
    private static String template = "# write you config here.";
    private static final Set<String> excludeConfig = ConcurrentHashMap.newKeySet();
    private static volatile List<ConfigListener> listeners;

    // init
    private static List<ConfigListener> getListeners() {
        if (listeners != null) {
            return listeners;
        }
        synchronized (ConfigManager.class) {
            if (listeners != null) {
                return listeners;
            }
            listeners = new ArrayList<>();
            ApplicationContext context = HBot.applicationContext;
            Map<String, ConfigListener> map = context.getBeansOfType(ConfigListener.class);
            for (Map.Entry<String, ConfigListener> entry : map.entrySet()) {
                if (entry.getValue() != null) {
                    listeners.add(entry.getValue());
                }
            }
        }
        return listeners;
    }

    /**
     * 添加排除在外的配置名，写回时将会忽略此配置
     *
     * @param cfgName 排除的配置名
     */
    public static void addExclude(String... cfgName) {
        excludeConfig.addAll(Set.of(cfgName));
    }

    /**
     * 指定一个配置类
     *
     * @param c 待指定的配置类
     */
    public static void setConfigClass(Class<?> c) {
        configClass = c;
    }

    /**
     * 设置模板内容，当配置不存在时，将填入此模板
     *
     * @param content 模板内容
     */
    public static void usingTemplate(@NonNull String content) {
        template = content;
    }

    /**
     * 设置模板文件资源，当配置文件不存在时，将填入以此模板文件资源
     *
     * @param url 资源 url
     */
    public static void usingTemplate(@NonNull URL url) throws IOException {
        InputStream stream = url.openConnection().getInputStream();
        usingTemplate(new String(stream.readAllBytes()));
    }

    /**
     * 加载 HRobot 指定的配置文件到指定的配置类中
     *
     * @throws Exception 任何问题
     */
    public synchronized static void loadConfig() throws Exception {
        if (configClass == null) {
            try {
                Map<String, Object> map = HBot.applicationContext.getBeansWithAnnotation(HBotConfigComponent.class);
                configClass = map.values().iterator().next().getClass();
                loadConfig();
            } catch (NoSuchBeanDefinitionException | NoSuchElementException e) {
                Logger.info("未指定程序配置类，将忽略加载默认配置文件过程");
            }
            return;
        }
        Yaml yaml = new Yaml();
        File file = new File(HBot.CONFIG_DIR, "config.yaml");
        Field[] fields = configClass.getDeclaredFields();
        // 如果配置文件存在
        if (file.exists()) {
            Map<?, ?> map;
            try (FileInputStream in = new FileInputStream(file)) {
                map = yaml.loadAs(in, Map.class);
            }
            if (map == null) {
                return;
            }
            // 反射设置 RobotConfig
            for (Field field : fields) {
                if (map.containsKey(field.getName())) {
                    try {
                        field.setAccessible(true);
                        Object value = map.get(field.getName());
                        Class<?> fieldClass = field.getType();
                        if (!fieldClass.equals(value.getClass())) {
                            if (Collection.class.isAssignableFrom(fieldClass) && !(value instanceof Collection)) {
                                value = List.of(value);
                            }
                            value = JSONObject.parseObject(JSONObject.toJSONString(value), fieldClass);
                        }
                        // 获取泛型参数
                        Type type = configClass.getDeclaredField(field.getName()).getGenericType();
                        if (type instanceof ParameterizedType) {
                            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
                            if (types != null && types.length > 0) {
                                if (value instanceof Collection<?>) {
                                    Collection<?> collection = new ArrayList<>();
                                    for (Object o : ((List<?>) value)) {
                                        collection.add(JSONObject.parseObject(JSONObject.toJSONString(o), types[0]));
                                    }
                                    value = collection;
                                }
                            }
                        }
                        // notify before
                        Object old = field.get(null);
                        if (old == null || !old.equals(value)) {
                            for (ConfigListener listener : getListeners()) {
                                if (listener.listenOn() == null || listener.listenOn().contains(field.getName())) {
                                    listener.actionBefore(old, value, field.getName());
                                }
                            }
                        }
                        field.setAccessible(true);
                        field.set(null, value);
                        // notify before
                        if (old == null || !old.equals(value)) {
                            for (ConfigListener listener : getListeners()) {
                                if (listener.listenOn() == null || listener.listenOn().contains(field.getName())) {
                                    listener.actionAfter(old, value, field.getName());
                                }
                            }
                        }
                    } catch (Exception ignored) {
                        // 可能是名字类型不符合，忽略
                    }
                }
            }
        } else {
            if (file.createNewFile()) {
                IOUtil.writeToFile(file, template);
                Logger.info("配置文件被创建，但未被加载，如需加载配置文件，请重启程序生效");
            }
        }
    }

    /**
     * 获取配置
     * @param fieldName
     * @return
     */
    public static Object getConfig(String fieldName) {
        try {
            Field field = configClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将配置转换为 map 的形式返回，这将排除 {@link #excludeConfig} 包含的对象
     *
     * @return 配置类转换的 Map 对象
     */
    public static Map<String, Object> getConfigMap() {
        try {
            Field[] fields = configClass.getDeclaredFields();
            Map<String, Object> map = new HashMap<>();
            for (Field field : fields) {
                if (!excludeConfig.contains(field.getName())) {
                    field.setAccessible(true);
                    map.put(field.getName(), field.get(null));
                }
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将配置动态写入配置文件
     */
    public synchronized static void writeConfig() throws Exception {
        Map<String, Object> map = JSONObject.parseObject(JSONObject.toJSONString(getConfigMap()));
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(HBot.joinPath(HBot.CONFIG_DIR, "config.yaml"))) {
            yaml.dump(map, writer);
        }
    }
}
