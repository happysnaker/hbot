package io.github.happysnaker.hbotcore.plugin;

import io.github.happysnaker.hbotcore.boot.BotStartInitializer;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.boot.HBotRunner;
import io.github.happysnaker.hbotcore.config.ConfigListener;
import io.github.happysnaker.hbotcore.config.ConfigManager;
import io.github.happysnaker.hbotcore.handler.*;
import io.github.happysnaker.hbotcore.intercept.Intercept;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.proxy.MessageHandlerProxy;
import io.github.happysnaker.hbotcore.utils.Pair;
import lombok.SneakyThrows;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 插件功能注册中心，用户可调用 {@link #register(HBotPluginEntry)} 和 {@link #unRegister(HBotPluginEntry)} 来注册或注销插件。
 * <p>对于插件提供方而言，可实现 {@link HBotPlugin} 接口在插件被扫描时调用此类提供的便捷方法向当前的 HBot 系统注册或注销自己</p>
 *
 * @Author happysnaker
 * @Date 2023/5/15
 * @Email happysnaker@foxmail.com
 * @see HBotPlugin
 * @see HBotPluginLoader
 */
public class HBotPluginRegister {


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
     * <p>如果插件中存在被 {@link io.github.happysnaker.hbotcore.config.HBotConfigComponent} 注解标注的类，HBot 会将其注册到 {@link ConfigManager} 中去</p>
     *
     * @param plugin
     */
    public static void register(HBotPluginEntry plugin) throws Exception {
        ClassLoader save = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(plugin.getClassLoader());

        registerSpringComponents(plugin);
        if (plugin.getConfigClass() != null && ConfigManager.pluginClass.stream()
                .noneMatch(c -> c.getName().equals(plugin.getConfigClass().getName()))) {
            ConfigManager.pluginClass.add(plugin.getConfigClass());
        }
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
                            method.invoke(getBean(c));
                        }
                    }
                    registerHBotComponent(c);
                }
            } else if (plugin.getPluginClass() != null) {
                plugin.getPluginClass()
                        .getConstructor()
                        .newInstance()
                        .start();
            } else {
                registerHandler(plugin.getHandlerList() == null ? new ArrayList<>() : plugin.getHandlerList());
                registerInterceptor(plugin.getInterceptorList() == null ? new ArrayList<>() : plugin.getInterceptorList());
                registerConfigListener(plugin.getConfigListenerList() == null ? new ArrayList<>() : plugin.getConfigListenerList());
                // staring 状态会自动运行一次 init
                if (HBotRunner.status == HBotRunner.RUNNING) {
                    for (Class<? extends BotStartInitializer> c : Optional.of(plugin.getInitializerList()).orElse(new ArrayList<>())) {
                        ((BotStartInitializer)getBean(c)).init();
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(save);
        }
    }

    /**
     * 在当前 HBot 系统中注销插件，同样有三种逻辑，可以看看 {@link #register(HBotPluginEntry)}
     *
     * @param plugin 插件名
     */
    public static void unRegister(String plugin) throws Exception {
        unRegister(HBotPluginLoader.get(plugin));
    }

    /**
     * 在当前 HBot 系统中注销插件，同样有三种逻辑，可以看看 {@link #register(HBotPluginEntry)}
     *
     * @param plugin 插件
     */
    public static void unRegister(HBotPluginEntry plugin) throws Exception {
        ClassLoader save = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(plugin.getClassLoader());
        if (plugin.getConfigClass() != null) {
            ConfigManager.pluginClass.remove(plugin.getConfigClass());
        }
        try {
            if (plugin.getCustomerClass() != null && !plugin.getCustomerClass().isEmpty()) {
                for (Pair<String, String> pair : plugin.getCustomerClass()) {
                    Class<?> c = plugin.getClassLoader().loadClass(pair.getKey());
                    registerHBotComponent(c);
                }
            } else if (plugin.getPluginClass() != null) {
                plugin.getPluginClass()
                        .getConstructor()
                        .newInstance()
                        .stop();
            } else {
                unRegisterHandler(plugin.getHandlerList() == null ? new ArrayList<>() : plugin.getHandlerList());
                unRegisterInterceptor(plugin.getInterceptorList() == null ? new ArrayList<>() : plugin.getInterceptorList());
                unRegisterConfigListener(plugin.getConfigListenerList() == null ? new ArrayList<>() : plugin.getConfigListenerList());
            }
        } finally {
            unRegisterSpringComponents(plugin);
            Thread.currentThread().setContextClassLoader(save);
        }
    }


    private static String getBeanName(Class<?> bean) {
        String name = bean.getSimpleName();
        return String.valueOf(name.charAt(0)).toLowerCase(Locale.ROOT) + name.substring(1);
    }

    private static boolean isBean(Class<?> bean, Set<Class<?>> visited) {
        Set<Class<?>> beanAnnotations = Set.of(
                Repository.class,
                Service.class,
                Controller.class,
                Configuration.class,
                Component.class,
                org.springframework.core.io.Resource.class
        );
        if (bean == null || visited.contains(bean)) {
            return false;
        }
        visited.add(bean);
//        @Retention、@Target、@Inherited、@Documented、@Repeatable 等。
        for (Annotation annotation : bean.getAnnotations()) {
            Class<? extends Annotation> type = annotation.annotationType();
            if (beanAnnotations.contains(type) || (type != bean && isBean(type, visited))) {
                return true;
            }
        }
        return false;
    }

    public static void unRegisterSpringComponents(HBotPluginEntry plugin) throws Exception {
        AutowireCapableBeanFactory factory0 = HBot.applicationContext.getAutowireCapableBeanFactory();
        DefaultListableBeanFactory factory1 = (DefaultListableBeanFactory) factory0;
        for (Class<?> c : plugin.getAllClass()) {
            if (isBean(c, new HashSet<>())) {
                factory1.destroyBean(getBeanName(c));
                factory1.removeBeanDefinition(getBeanName(c));
            }
        }
    }

    public static void registerSpringComponents(HBotPluginEntry plugin) throws Exception {
        AutowireCapableBeanFactory factory0 = HBot.applicationContext.getAutowireCapableBeanFactory();
        DefaultListableBeanFactory factory1 = (DefaultListableBeanFactory) factory0;
        for (Class<?> c : plugin.getAllClass()) {
            if (isBean(c, new HashSet<>())) {
                BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(c);
                BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();
                beanDefinition.setScope("singleton");
                factory1.registerBeanDefinition(getBeanName(c), beanDefinition);
            }
        }
        ClassLoader classLoader = factory1.getBeanClassLoader();
        factory1.setBeanClassLoader(plugin.getClassLoader());
        for (Class<?> c : plugin.getAllClass()) {
            if (isBean(c, new HashSet<>())) {

                Object bean = factory1.getBean(getBeanName(c));
            }
        }
        factory1.setBeanClassLoader(classLoader);
    }

    @SneakyThrows
    private static Object getBean(Class<?> c) {
        try {
            return HBot.applicationContext.getBean(getBeanName(c));
        } catch (NoSuchBeanDefinitionException e) {
            return getBean(c);
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
    public static void registerHBotComponent(Object obj) throws Exception {
        registerHBotComponent(obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
    }

    /**
     * 注册 HBot 组件
     * <br>
     * 如果是 {@link io.github.happysnaker.hbotcore.boot.BotStartInitializer}，那么将会立即执行初始化方法
     *
     * @param c 组件 class
     * @throws Exception
     */
    public static void registerHBotComponent(Class<?> c) throws Exception {
        if (MessageEventHandler.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            registerHandler0(Collections.singletonList((MessageEventHandler) getBean(c)));
        }
        if (Interceptor.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            registerInterceptor0(Collections.singletonList((Interceptor) getBean(c)));
        }
        if (ConfigListener.class.isAssignableFrom(c)) {
            registerConfigListener0(Collections.singletonList((ConfigListener) getBean(c)));
        }
        if (BotStartInitializer.class.isAssignableFrom(c)) {
            ((BotStartInitializer) getBean(c)).init();
        }
    }


    /**
     * 注销 HBot 组件
     * <br>
     *
     * @param obj 组件
     * @throws Exception
     */
    public static void unRegisterHBotComponent(Object obj) throws Exception {
        unRegisterHBotComponent(obj instanceof Class<?> ? (Class<?>) obj : obj.getClass());
    }

    /**
     * 注销 HBot 组件
     * <br>
     *
     * @param c 组件 class
     * @throws Exception
     */
    public static void unRegisterHBotComponent(Class<?> c) throws Exception {
        if (MessageEventHandler.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            unRegisterHandler0(Collections.singletonList((MessageEventHandler) getBean(c)));
        }
        if (Interceptor.class.isAssignableFrom(c) && c.isAnnotationPresent(handler.class)) {
            unRegisterInterceptor0(Collections.singletonList((Interceptor) getBean(c)));
        }
        if (ConfigListener.class.isAssignableFrom(c)) {
            unRegisterConfigListener0(Collections.singletonList((ConfigListener) getBean(c)));
        }

    }


    /**
     * 添加一些处理器到现有的 HBot 系统中，由插件提供方调用
     *
     * @param handlers 处理器
     */
    public static void registerHandler(List<Class<? extends MessageEventHandler>> handlers) {
        registerHandler0(handlers.stream()
                .map(h -> (MessageEventHandler) getBean(h))
                .collect(Collectors.toList()));
    }

    /**
     * 添加一些处理器到现有的 HBot 系统中，由插件提供方调用
     *
     * @param handlers 处理器
     */
    @SneakyThrows
    public static void registerHandler0(List<MessageEventHandler> handlers) {
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
            if (handler.getClass().isAnnotationPresent(InterestFilter.class)) {
                InterestFilterPostProcessor.injectInterestFilter(handler);
            }
            if (handler.getClass().isAnnotationPresent(InterestFilters.class)) {
                InterestFilterPostProcessor.injectInterestFilters(handler);
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
     * 添加一些拦截器到现有的 HBot 系统中，由插件提供方调用
     *
     * @param interceptors 拦截器
     */
    public static void registerInterceptor(List<Class<? extends Interceptor>> interceptors) {
        registerInterceptor0(interceptors.stream()
                .map(i -> (Interceptor) getBean(i))
                .collect(Collectors.toList()));
    }

    /**
     * 添加一些拦截器到现有的 HBot 系统中，由插件提供方调用
     *
     * @param interceptors 拦截器
     */
    public static void registerInterceptor0(List<Interceptor> interceptors) {
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
    public static void registerConfigListener(List<Class<? extends ConfigListener>> configListeners) {
        registerConfigListener0(configListeners.stream()
                .map(cl -> (ConfigListener) getBean(cl))
                .collect(Collectors.toList()));
    }

    /**
     * 添加一些配置监听器到现有的 HBot 系统之中
     *
     * @param configListeners 配置监听器
     */
    @SneakyThrows
    public static void registerConfigListener0(List<ConfigListener> configListeners) {
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
    public static void unRegisterHandler(List<Class<? extends MessageEventHandler>> handlers) {
        MessageHandlerProxy proxy = (MessageHandlerProxy) HBot.applicationContext.getBean("proxyHandler");
        handlers.forEach(del -> removeOnClassEquals(proxy.getHandlers(), del.getName()));
    }

    /**
     * 从当前的 HBot 系统中移除一些处理器
     *
     * @param handlers 处理器
     */
    public static void unRegisterHandler0(List<MessageEventHandler> handlers) {
        unRegisterHandler(handlers.stream().map(MessageEventHandler::getClass).collect(Collectors.toList()));
    }


    /**
     * 从当前的 HBot 系统中移除一些拦截器
     *
     * @param interceptors 处理器
     */
    public static void unRegisterInterceptor(List<Class<? extends Interceptor>> interceptors) {
        MessageHandlerProxy proxy = (MessageHandlerProxy) HBot.applicationContext.getBean("proxyHandler");
        interceptors.forEach(del -> removeOnClassEquals(proxy.getPreInterceptors(), del.getName()));
        interceptors.forEach(del -> removeOnClassEquals(proxy.getPostInterceptors(), del.getName()));
    }

    /**
     * 从当前的 HBot 系统中移除一些拦截器
     *
     * @param interceptors 处理器
     */
    public static void unRegisterInterceptor0(List<Interceptor> interceptors) {
        unRegisterInterceptor(interceptors.stream().map(Interceptor::getClass).collect(Collectors.toList()));
    }

    /**
     * 从当前 HBot 系统中移除一些配置监听器
     *
     * @param listeners 配置监听器
     */
    @SneakyThrows
    public static void unRegisterConfigListener(List<Class<? extends ConfigListener>> listeners) {
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
    public static void unRegisterConfigListener0(List<ConfigListener> listeners) {
        unRegisterConfigListener(listeners.stream().map(ConfigListener::getClass).collect(Collectors.toList()));
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
