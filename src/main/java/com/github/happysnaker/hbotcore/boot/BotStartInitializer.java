package com.github.happysnaker.hbotcore.boot;

/**
 * 用于暴露开发者初始化机器人的接口，请确保<strong>实现类被 Spring 所扫描</strong>
 * @Author happysnaker
 * @Date 2023/3/3
 * @Email happysnaker@foxmail.com
 */
public interface BotStartInitializer {
    /**
     * 允许开发者提供自己的逻辑以初始化机器人，此方法调用时机是加载配置之后，登录机器人、监听事件之前调用<p>
     *     用户可在此处检查配置，监听其他事件，手动登录，配置一些额外逻辑
     * </p>
     */
    void init();
}
