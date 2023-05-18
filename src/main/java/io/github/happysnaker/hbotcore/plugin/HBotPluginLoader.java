package io.github.happysnaker.hbotcore.plugin;

import io.github.happysnaker.hbotcore.boot.BotStartInitializer;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.config.ConfigListener;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.handler.handler;
import io.github.happysnaker.hbotcore.intercept.Intercept;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.Pair;
import org.springframework.boot.loader.JarLauncher;
import org.springframework.boot.loader.LaunchedURLClassLoader;
import org.springframework.boot.loader.archive.Archive;
import org.springframework.boot.loader.archive.JarFileArchive;
import org.springframework.boot.loader.jar.JarFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

/**
 * HBot 插件加载器，调用此类的 {@link #load(File)} 方法来加载一个插件 jar 包，将返回的插件信息拿到 {@link HBotPluginRegistry} 处注册，即可使用插件提供的功能
 * <p>调用 {@link #unLoad(String)} 可以卸载类重而达到热重载的功能，请勿忘了到 {@link HBotPluginRegistry} 处注销登记</p>
 * <p>此类与 {@link HBotPluginRegistry} 的区别在于，<strong>此类提供关于类的加载与卸载，而 {@link HBotPluginRegistry} 负责将插件功能插拔到 HBot 中去</strong></p>
 * <p>加载插件的顺序是先加载插件的类，在加载插件的功能；而注销插件的顺序是先注销插件功能，再卸载插件的类</p>
 *
 * @Author happysnaker
 * @Date 2023/5/14
 * @Email happysnaker@foxmail.com
 * @see HBotPluginRegistry
 */
public class HBotPluginLoader extends JarLauncher {
    private final File jarFile;

    private HBotPluginLoader(File jarFile) throws IOException {
        super(new JarFileArchive(jarFile));
        this.jarFile = jarFile;
    }

    private HBotPluginEntry load() throws Exception {
        if (!isExploded()) {
            JarFile.registerUrlProtocolHandler();
        }
        Iterator<Archive> iterator = getClassPathArchivesIterator();
        URLClassLoader classLoader = null;

        HBotPluginEntry plugin = new HBotPluginEntry();
        JarFile jar = new JarFile(jarFile);
        plugin.setJarFile(jar);
        plugin.setName(jarFile.getName().replace(".jar", ""));
        Enumeration<JarEntry> entries = jar.entries();
        List<Class<?>> classList = new ArrayList<>();
        boolean isSpringBoot = false;

        // only sacn BOOT-IBF/
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class") && entry.getName().startsWith(getArchiveEntryPathPrefix())) {
                String className = entry.getName()
                        .replace(".class", "")
                        .replace("BOOT-INF/classes/", "")
                        .replace("/", ".");
                isSpringBoot = true;
                if (classLoader == null) {
                    classLoader = (LaunchedURLClassLoader) createClassLoader(iterator);
                }
                try {
                    Class<?> aClass = classLoader.loadClass(className);
                    if (HBotPlugin.class.isAssignableFrom(aClass)) {
                        plugin.setPluginClass((Class<? extends HBotPlugin>) aClass);
                    } else {
                        classList.add(aClass);
                    }
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    continue;
                }
            }
        }

        // not springboot, using normal url classloader
        if (!isSpringBoot) {
            classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, Thread.currentThread().getContextClassLoader());
            entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class")) {
                    String className = entry.getName()
                            .replace(".class", "")
                            .replace("/", ".");
                    try {
                        Class<?> aClass = classLoader.loadClass(className);
                        if (HBotPlugin.class.isAssignableFrom(aClass)) {
                            plugin.setPluginClass((Class<? extends HBotPlugin>) aClass);
                        } else {
                            classList.add(aClass);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError e) {
                        continue;
                    }
                }
            }
        }
        plugin.setClassLoader(classLoader);
        plugin.setHandlerList(classList.stream()
                .filter(c -> c.isAnnotationPresent(handler.class))
                .filter(MessageEventHandler.class::isAssignableFrom)
                .map(c -> (Class<MessageEventHandler>) c)
                .collect(Collectors.toList()));
        plugin.setInterceptorList(classList.stream()
                .filter(c -> c.isAnnotationPresent(Intercept.class))
                .filter(Interceptor.class::isAssignableFrom)
                .map(c -> (Class<Interceptor>) c)
                .collect(Collectors.toList()));
        plugin.setInitializerList(classList.stream()
                .filter(BotStartInitializer.class::isAssignableFrom)
                .map(c -> (Class<BotStartInitializer>) c)
                .collect(Collectors.toList()));
        plugin.setConfigListenerList(classList.stream()
                .filter(ConfigListener.class::isAssignableFrom)
                .map(c -> (Class<ConfigListener>) c)
                .collect(Collectors.toList()));
        File application = new File(HBot.joinPath(HBot.PLUGIN_DIR, jarFile.getName().replace(".jar", ".txt")));
        if (application.exists()) {
            plugin.setCustomerClass(new ArrayList<>());
            for (String line : Files.readAllLines(application.toPath())) {
                if (line.trim().startsWith("#")) {
                    continue;
                }
                String[] split = line.trim().split("#");
                if (split.length == 1) {
                    plugin.getCustomerClass().add(Pair.of(split[0], null));
                } else if (split.length == 2) {
                    plugin.getCustomerClass().add(Pair.of(split[0], split[1]));
                }
            }
        }
        return plugin;
    }

    private static final Map<String, HBotPluginEntry> pluginMap = new HashMap<>();

    /**
     * 加载一个插件，如果已经加载过一次，则不会重新加载，除非它被注销
     *
     * @param name 插件名，对应的 jarFile 为 PLUGIN_DIR/name.jar
     * @return 插件信息，可前往 {@link HBotPluginRegistry} 注册
     * @throws Exception
     */
    public static HBotPluginEntry load(String name) throws Exception {
        return load(new File(HBot.joinPath(HBot.PLUGIN_DIR, name + ".jar")));
    }

    /**
     * 加载一个插件，如果已经加载过一次，则不会重新加载，除非它被注销
     *
     * @param jarFile 插件文件
     * @return 插件信息，可前往 {@link HBotPluginRegistry} 注册
     * @throws Exception
     */
    public static HBotPluginEntry load(File jarFile) throws Exception {
        String name = jarFile.getName().replace(".jar", "");
        if (pluginMap.get(name) != null) {
            return pluginMap.get(name);
        }
        HBotPluginLoader pluginManager = new HBotPluginLoader(jarFile);
        HBotPluginEntry pluginEntry = pluginManager.load();
        pluginMap.put(pluginEntry.getName(), pluginEntry);
        Logger.info("load plugin " + name + " successfully, plugin is " + pluginEntry);
        return pluginEntry;
    }

    /**
     * 卸载一个插件
     * @param name 插件名
     * @throws IOException
     */
    public static void unLoad(String name) throws IOException {
        unLoad(pluginMap.get(name));
    }

    /**
     * 卸载一个插件
     * @param plugin 插件
     * @throws IOException
     */
    public static void unLoad(HBotPluginEntry plugin) throws IOException {
        Logger.info("unload plugin " + plugin.getName());
        plugin.getJarFile().close();
        plugin.getClassLoader().close();
        pluginMap.remove(plugin.getName());
        // try clear
        System.gc();
    }


    /**
     * 获取一个已被加载但未被注销的插件
     *
     * @param pluginName 插件名
     * @return 插件信息
     */
    public static HBotPluginEntry get(String pluginName) {
        if (pluginMap.get(pluginName) != null) {
            return pluginMap.get(pluginName);
        }
        throw new NoSuchElementException();
    }
}


