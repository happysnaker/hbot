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
import java.nio.charset.StandardCharsets;
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
     * 插件中的配置类
     */
    public static final List<Class<?>> pluginClass = new ArrayList<>();
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
        File file = new File(HBot.CONFIG_DIR, "config.yaml");
        if (!file.exists()) {
            if (file.createNewFile()) {
                IOUtil.writeToFile(file, template);
                Logger.warning("配置文件被创建，请填写配置文件并重启程序生效");
            }
        }
        // load plugin
        for (Class<?> aClass : pluginClass) {
            loadConfig(aClass, file);
        }
        if (configClass == null) {
            try {
                Map<String, Object> map = HBot.applicationContext.getBeansWithAnnotation(HBotConfigComponent.class);
                configClass = map.values().iterator().next().getClass();
                loadConfig(configClass, file);
            } catch (NoSuchBeanDefinitionException | NoSuchElementException e) {
                Logger.info("未指定程序配置类，将忽略加载默认配置文件过程");
            }
        } else {
            loadConfig(configClass, file);
        }
    }

    /**
     * 从指定文件加载到指定类中
     *
     * @param configClass
     * @param file
     * @throws Exception
     */
    public synchronized static void loadConfig(Class<?> configClass, File file) throws Exception {
        Yaml yaml = new Yaml();
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
                                if (value instanceof Collection coll) {
                                    boolean shouldTransform = !coll.isEmpty() &&
                                            (!(types[0] instanceof Class) || !((Class) types[0]).isAssignableFrom(coll.iterator().next().getClass()));
                                    if (shouldTransform) {
                                        Collection<Object> collection = new ArrayList<>();
                                        for (Object o : coll) {
                                            collection.add(JSONObject.parseObject(JSONObject.toJSONString(o), types[0]));
                                        }
                                        coll.clear();
                                        coll.addAll(collection);
                                    }
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
        }
    }

    /**
     * 获取配置（包含插件配置），如果与插件冲突，则以主配置类为主
     *
     * @param fieldName 配置名
     * @return
     */
    public static Object getConfig(String fieldName) {
        try {
            Field field = configClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception e) {
            for (Class<?> aClass : pluginClass) {
                try {
                    Field field = aClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(null);
                } catch (Exception ex) {
                    continue;
                }
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * 将配置转换为 map 的形式返回（包含插件），这将排除 {@link #excludeConfig} 包含的对象
     *
     * @return 配置类转换的 Map 对象
     */
    public static Map<String, Object> getConfigMap() {
        Map<String, Object> map = configClass == null ? new HashMap<>() : getConfigMap(configClass);
        for (Class<?> aClass : pluginClass) {
            Map<String, Object> plugin = getConfigMap(aClass);
            for (Map.Entry<String, Object> it : plugin.entrySet()) {
                if (!map.containsKey(it.getKey())) {
                    map.put(it.getKey(), it.getValue());
                }
            }
        }
        return map;
    }

    /**
     * 将指定配置转换为 map 的形式返回，这将排除 {@link #excludeConfig} 包含的对象
     *
     * @return 配置类转换的 Map 对象
     */
    public static Map<String, Object> getConfigMap(Class<?> configClass) {
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
     * 将配置动态(包含插件)写入配置文件
     */
    public synchronized static void writeConfig() throws Exception {
        Map<String, Object> map = JSONObject.parseObject(JSONObject.toJSONString(getConfigMap()));
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(HBot.joinPath(HBot.CONFIG_DIR, "config.yaml"), StandardCharsets.UTF_8)) {
            yaml.dump(map, writer);
        }
    }

    /**
     * 将指定的配置类写回到指定的文件中
     */
    public synchronized static void writeConfig(Class<?> configClass, String fileName) throws Exception {
        Map<String, Object> map = JSONObject.parseObject(
                JSONObject.toJSONString(getConfigMap(configClass)));
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(fileName)) {
            yaml.dump(map, writer);
        }
    }
}
