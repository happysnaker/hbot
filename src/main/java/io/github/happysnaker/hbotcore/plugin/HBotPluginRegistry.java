package io.github.happysnaker.hbotcore.plugin;

import io.github.happysnaker.hbotcore.boot.BotStartInitializer;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.config.ConfigListener;
import io.github.happysnaker.hbotcore.config.ConfigManager;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.handler.handler;
import io.github.happysnaker.hbotcore.intercept.Intercept;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.proxy.MessageHandlerProxy;
import io.github.happysnaker.hbotcore.utils.Pair;
import lombok.SneakyThrows;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 插件功能注册中心，用户可调用 {@link #registry(HBotPluginEntry)} 和 {@link #unRegistry(HBotPluginEntry)} 来注册或注销插件。
 * <p>对于插件提供方而言，可实现 {@link HBotPlugin} 接口在插件被扫描时调用此类提供的便捷方法向当前的 HBot 系统注册或注销自己</p>
 *
 * @Author happysnaker
 * @Date 2023/5/15
 * @Email happysnaker@foxmail.com
 * @see HBotPlugin
 * @see HBotPluginLoader
 */
public class HBotPluginRegistry {


    /**
     * 注册一个插件到 HBot 中，根据具体的情景，可能会有三种逻辑：
     * <ol>
     *     <li>如果在插件目录下存在一个与插件同名的 txt 文件，那么用户可以在 txt 文件中配置要添加的 HBot 组件或执行自定义方法，
     *              如果文件内容非空，那么此方法将会优先执行此逻辑，对于 HBot 组件，此方法会将其添加到当前系统中，对于自定义方法此方法将立即执行</li>
     *     <li>如果没有配置相关 txt 文件，但是插件中存在 {@link HBotPlugin} 接口，那么此方法会执行 {@link HBotPlugin#start()} 方法</li>
     *     <li>如果既没有配置相关 txt 文件，也不存在 {@link HBotPlugin} 接口，那么此方法会将所有的 HBot 组件添加到 HBot 系统当中去</li>
     * </ol>
     * 其中 HBot 组件包括 {@link MessageEventHandler}、{@link Interceptor}、{@link io.github.happysnaker.hbotcore.boot.BotStartInitializer}、{@link ConfigListener}
     * <p>对于 {@link io.github.happysnaker.hbotcore.boot.BotStartInitializer} 组件，会立即执行其中的 init 方法</p>
     *
     * @param plugin
     */
    public static void registry(HBotPluginEntry plugin) throws Exception {
        ClassLoader save = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(plugin.getClassLoader());
        try {
            if (plugin.getCustomerClass() != null && !plugin.getCustomerClass().isEmpty()) {
                for (Pair<String, String> pair : plugin.getCustomerClass()) {
                    Class<?> c = plugin.getClassLoader().loadClass(pair.getKey());
                    if (pair.getValue() != null) {
                        Method method = c.getDeclaredMethod(pair.getValue());
                        method.setAccessible(true);
                        try {
                            method.invoke(null);
                        } catch (NullPointerException e) {
                            method.invoke(c.getConstructor().newInstance());
                        }
                    }
                    registryHBotComponent(c);
                }
            } else if (plugin.getPluginClass() != null) {
                plugin.getPluginClass()
                        .getConstructor()
                        .newInstance()
                        .start();
            } else {
                registryHandler(plugin.getHandlerList() == null ? new ArrayList<>() : plugin.getHandlerList());
                registryInterceptor(plugin.getInterceptorList() == null ? new ArrayList<>() : plugin.getInterceptorList());
                registryConfigListener(plugin.getConfigListenerList() == null ? new ArrayList<>() : plugin.getConfigListenerList());
                for (Class<? extends BotStartInitializer> c : Optional.of(plugin.getInitializerList()).orElse(new ArrayList<>())) {
                    c.getConstructor().newInstance().init();
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(save);
        }
    }

    /**
     * 在当前 HBot 系统中注销插件，同样有三种逻辑，可以看看 {@link #registry(HBotPluginEntry)}
     *
     * @param plugin 插件名
     */
    public static void unRegistry(String plugin) throws Exception {
        unRegistry(HBotPluginLoader.get(plugin));
    }

    /**
     * 在当前 HBot 系统中注销插件，同样有三种逻辑，可以看看 {@link #registry(HBotPluginEntry)}
     *
     * @param plugin 插件
     */
    public static void unRegistry(HBotPluginEntry plugin) throws Exception {
        if (plugin.getCustomerClass() != null && !plugin.getCustomerClass().isEmpty()) {
            for (Pair<String, String> pair : plugin.getCustomerClass()) {
                Class<?> c = plugin.getClassLoader().loadClass(pair.getKey());
                registryHBotComponent(c);
            }
        } else if (plugin.getPluginClass() != null) {
            plugin.getPluginClass()
                    .getConstructor()
                    .newInstance()
                    .stop();
        } else {
            unRegistryHandler(plugin.getHandlerList() == null ? new ArrayList<>() : plugin.getHandlerList());
            unRegistryInterceptor(plugin.getInterceptorList() == null ? new ArrayList<>() : plugin.getInterceptorList());
            unRegistryConfigListener(plugin.getConfigListenerList() == null ? new ArrayList<>() : plugin.getConfigListenerList());
        }
    }

    /**
     * 注册 HBot 组件
     * <br>
     * 如果是 {@link io.github.happysnaker.hbotcore.boot.BotStartInitializer}，那么将会立即执行初始化方法
     *
     * @param obj 组件
     * @throws Exception
     */
    public static void registryHBotComponent(Object obj) throws Exception {
        registryHBotComponent(obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
    }

    /**
     * 注册 HBot 组件
     * <br>
     * 如果是 {@link io.github.happysnaker.hbotcore.boot.BotStartInitializer}，那么将会立即执行初始化方法
     *
     * @param c 组件 class
     * @throws Exception
     */
    public static void registryHBotComponent(Class<?> c) throws Exception {
        if (MessageEventHandler.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            registryHandler0(Collections.singletonList((MessageEventHandler) c.getConstructor().newInstance()));
        }
        if (Interceptor.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            registryInterceptor0(Collections.singletonList((Interceptor) c.getConstructor().newInstance()));
        }
        if (ConfigListener.class.isAssignableFrom(c)) {
            registryConfigListener0(Collections.singletonList((ConfigListener) c.getConstructor().newInstance()));
        }
        if (BotStartInitializer.class.isAssignableFrom(c)) {
            ((BotStartInitializer) c.getConstructor().newInstance()).init();
        }
    }


    /**
     * 注销 HBot 组件
     * <br>
     *
     * @param obj 组件
     * @throws Exception
     */
    public static void unRegistryHBotComponent(Object obj) throws Exception {
        unRegistryHBotComponent(obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
    }

    /**
     * 注销 HBot 组件
     * <br>
     *
     * @param c 组件 class
     * @throws Exception
     */
    public static void unRegistryHBotComponent(Class<?> c) throws Exception {
        if (MessageEventHandler.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            unRegistryHandler0(Collections.singletonList((MessageEventHandler) c.getConstructor().newInstance()));
        }
        if (Interceptor.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            unRegistryInterceptor0(Collections.singletonList((Interceptor) c.getConstructor().newInstance()));
        }
        if (ConfigListener.class.isAssignableFrom(c)) {
            unRegistryConfigListener0(Collections.singletonList((ConfigListener) c.getConstructor().newInstance()));
        }

    }


    /**
     * 添加一些处理器到现有的 HBot 系统中，由插件提供方调用
     *
     * @param handlers 处理器
     */
    public static void registryHandler(List<Class<? extends MessageEventHandler>> handlers) {
        registryHandler0(handlers.stream().map(h -> {
            try {
                return h.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }

    /**
     * 添加一些处理器到现有的 HBot 系统中，由插件提供方调用
     *
     * @param handlers 处理器
     */
    public static void registryHandler0(List<MessageEventHandler> handlers) {
        if (handlers.stream().anyMatch(c -> !(c.getClass().isAnnotationPresent(handler.class)))) {
            throw new IllegalArgumentException("There are some handlers not annotation with handler");
        }
        MessageHandlerProxy proxy = (MessageHandlerProxy) HBot.applicationContext.getBean("proxyHandler");
        for (MessageEventHandler handler : handlers) {
            if (proxy.getHandlers()
                    .stream()
                    .anyMatch(curr -> curr.getClass().getName().equals(handler.getClass().getName()))) {
                continue;
            }
            proxy.getHandlers().add(handler);
        }
        proxy.getHandlers().sort((a, b) -> {
            handler annotation0 = a.getClass().getAnnotation(handler.class);
            handler annotation1 = b.getClass().getAnnotation(handler.class);
            if (annotation0.isCommandHandler() || annotation1.isCommandHandler()) {
                if (!annotation0.isCommandHandler() || !annotation1.isCommandHandler()) {
                    return annotation0.isCommandHandler() ? -1 : 1;
                }
            }
            int aa = annotation0.priority();
            int bb = annotation1.priority();
            return bb - aa;
        });
    }


    /**
     * 添加一些拦截到现有的 HBot 系统中，由插件提供方调用
     *
     * @param interceptors 拦截器
     */
    public static void registryInterceptor(List<Class<? extends Interceptor>> interceptors) {
        registryInterceptor0(interceptors.stream().map(i -> {
            try {
                return i.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()));
    }

    /**
     * 添加一些拦截到现有的 HBot 系统中，由插件提供方调用
     *
     * @param interceptors 拦截器
     */
    public static void registryInterceptor0(List<Interceptor> interceptors) {
        if (interceptors.stream().anyMatch(c -> !(c.getClass().isAnnotationPresent(Intercept.class)))) {
            throw new IllegalArgumentException("There are some handlers not annotation with handler");
        }
        MessageHandlerProxy proxy = (MessageHandlerProxy) HBot.applicationContext.getBean("proxyHandler");
        for (var interceptor : interceptors) {
            if (proxy.getPreInterceptors()
                    .stream()
                    .noneMatch(curr -> curr.getClass().getName().equals(interceptor.getClass().getName()))) {
                proxy.getPreInterceptors().add(interceptor);
            }
            if (proxy.getPostInterceptors()
                    .stream()
                    .noneMatch(curr -> curr.getClass().getName().equals(interceptor.getClass().getName()))) {
                proxy.getPostInterceptors().add(interceptor);
            }
        }
        proxy.getPreInterceptors().sort((a, b) -> {
            int aa = a.getClass().getAnnotation(Intercept.class).preOrder();
            int bb = b.getClass().getAnnotation(Intercept.class).preOrder();
            return bb - aa;
        });
        proxy.getPostInterceptors().sort((a, b) -> {
            int aa = a.getClass().getAnnotation(Intercept.class).postOrder();
            int bb = b.getClass().getAnnotation(Intercept.class).postOrder();
            return bb - aa;
        });
    }

    /**
     * 添加一些配置监听器到现有的 HBot 系统之中
     *
     * @param configListeners 配置监听器
     */
    @SneakyThrows
    public static void registryConfigListener(List<Class<? extends ConfigListener>> configListeners) {
        registryConfigListener0(configListeners.stream()
                .map(cl -> {
                    try {
                        return cl.getConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList()));
    }

    /**
     * 添加一些配置监听器到现有的 HBot 系统之中
     *
     * @param configListeners 配置监听器
     */
    @SneakyThrows
    public static void registryConfigListener0(List<ConfigListener> configListeners) {
        Method method = ConfigManager.class.getDeclaredMethod("getListeners");
        method.setAccessible(true);
        List<ConfigListener> systemListeners = (List<ConfigListener>) method.invoke(null);
        for (ConfigListener listener : configListeners) {
            if (systemListeners.stream().
                    noneMatch(s -> s.getClass().getName().equals(listener.getClass().getName()))) {
                systemListeners.add(listener);
            }
        }
    }


    /**
     * 从当前的 HBot 系统中移除一些处理器
     *
     * @param handlers 处理器
     */
    public static void unRegistryHandler(List<Class<? extends MessageEventHandler>> handlers) {
        MessageHandlerProxy proxy = (MessageHandlerProxy) HBot.applicationContext.getBean("proxyHandler");
        handlers.forEach(del -> removeOnClassEquals(proxy.getHandlers(), del.getName()));
    }

    /**
     * 从当前的 HBot 系统中移除一些处理器
     *
     * @param handlers 处理器
     */
    public static void unRegistryHandler0(List<MessageEventHandler> handlers) {
        unRegistryHandler(handlers.stream().map(MessageEventHandler::getClass).collect(Collectors.toList()));
    }


    /**
     * 从当前的 HBot 系统中移除一些拦截器
     *
     * @param interceptors 处理器
     */
    public static void unRegistryInterceptor(List<Class<? extends Interceptor>> interceptors) {
        MessageHandlerProxy proxy = (MessageHandlerProxy) HBot.applicationContext.getBean("proxyHandler");
        interceptors.forEach(del -> removeOnClassEquals(proxy.getPreInterceptors(), del.getName()));
        interceptors.forEach(del -> removeOnClassEquals(proxy.getPostInterceptors(), del.getName()));
    }

    /**
     * 从当前的 HBot 系统中移除一些拦截器
     *
     * @param interceptors 处理器
     */
    public static void unRegistryInterceptor0(List<Interceptor> interceptors) {
        unRegistryInterceptor(interceptors.stream().map(Interceptor::getClass).collect(Collectors.toList()));
    }

    /**
     * 从当前 HBot 系统中移除一些配置监听器
     *
     * @param listeners 配置监听器
     */
    @SneakyThrows
    public static void unRegistryConfigListener(List<Class<? extends ConfigListener>> listeners) {
        Method method = ConfigManager.class.getDeclaredMethod("getListeners");
        method.setAccessible(true);
        List<ConfigListener> systemListeners = (List<ConfigListener>) method.invoke(null);
        listeners.forEach(del -> removeOnClassEquals(systemListeners, del.getName()));
    }


    /**
     * 从当前 HBot 系统中移除一些配置监听器
     *
     * @param listeners 配置监听器
     */
    @SneakyThrows
    public static void unRegistryConfigListener0(List<ConfigListener> listeners) {
        unRegistryConfigListener(listeners.stream().map(ConfigListener::getClass).collect(Collectors.toList()));
    }

    private static void removeOnClassEquals(List<?> origin, String delClassName) {
        int delIndex = -1;
        for (int i = 0; i < origin.size(); i++) {
            if (origin.get(i).getClass().getName().equals(delClassName)) {
                delIndex = i;
                break;
            }
        }
        if (delIndex >= 0) {
            origin.remove(delIndex);
        }
    }
}
