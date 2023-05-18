package io.github.happysnaker.hbotcore.plugin;

/**
 * HBot 的插件核心组件，能够让用户自定义插件启动与销毁时的逻辑
 * @Author happysnaker
 * @Date 2023/5/15
 * @Email happysnaker@foxmail.com
 * @see HBotPluginRegistry
 */
public interface HBotPlugin {
    /**
     * 插件被加载时的逻辑
     */
    void start() throws Exception;

    /**
     * 插件被注销时的逻辑
     */
    void stop() throws Exception;
}
