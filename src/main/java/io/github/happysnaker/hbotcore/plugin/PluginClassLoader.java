package io.github.happysnaker.hbotcore.plugin;


/**
 * @Author happysnaker
 * @Date 2023/5/21
 * @Email happysnaker@foxmail.com
 */
public class PluginClassLoader extends ClassLoader {
    private volatile static PluginClassLoader instance;

    public static PluginClassLoader getInstance(Class<?> superClass) {
        if (instance != null) {
            return instance;
        }
        synchronized (PluginClassLoader.class) {
            instance = new PluginClassLoader(superClass);
        }
        return instance;
    }

    Class<?> superClass;

    private PluginClassLoader(Class<?> superClass) {
        super(superClass.getClassLoader());
        this.superClass = superClass;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            // 优先使用主自动类的加载器
            return superClass.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e0) {
            try {
                // 否则使用线程上下文加载
                return super.loadClass(name);
            } catch (ClassNotFoundException e1) {
                // 插件加载
                for (HBotPluginEntry plugin : HBotPluginLoader.getPlugins()) {
                    try {
                        return plugin.getClassLoader().loadClass(name);
                    } catch (ClassNotFoundException ignore) {
                        // ignore
                    }
                }
                throw e1;
            }
        }
    }
}
