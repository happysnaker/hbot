package com.github.happysnaker.hbotcore.config;

import java.util.Set;

/**
 * 配置监听器，<strong>当 {@link ConfigManager#loadConfig()} 被调用，并且配置文件中的配置项与配置类中的属性值不符合时，监听器被触发。</strong>
 * <p>换句话说，当配置重载时，可以通过监听器监听配置变化。如果无需求，则可不配置，但某些情况监听器可能很有用，</p>
 * 监听器不需要手动配置，只要实现的监听器在 Spring 工厂中，则会被自动装载
 *
 * @Author happysnaker
 * @Date 2023/2/25
 * @Email happysnaker@foxmail.com
 */
public interface ConfigListener {

    /**
     * 表明监听器对哪些配置感兴趣，此方法返回一些配置名，如果不为 null，则仅会在其感兴趣的配置上调用它
     *
     * @return 返回监听器感兴趣的配置，如果对所有配置感兴趣，请返回 null
     */
    default Set<String> listenOn() {
        return null;
    }

    /**
     * 当重载配置文件时，检测到配置变更将调用此方法
     * <p>此方法在 UPDATE 配置类之前调用</p>
     * @param oldData 原先的数据，请注意在第一次加载时，oldData 总是为 null
     * @param newData 变更后的数据
     * @param name    变更的数据名
     */
    void actionBefore(Object oldData, Object newData, String name);

    /**
     * 当重载配置文件时，检测到配置变更将调用此方法
     * <p>此方法在 UPDATE 配置类之后调用</p>
     * @param oldData 原先的数据，请注意在第一次加载时，oldData 总是为 null
     * @param newData 变更后的数据
     * @param name    变更的数据名
     */
    void actionAfter(Object oldData, Object newData, String name);
}
