package com.github.happysnaker.hbotcore.boot;


import com.github.happysnaker.hbotcore.config.ConfigManager;
import com.github.happysnaker.hbotcore.cron.HBotCronJob;
import com.github.happysnaker.hbotcore.proxy.MessageHandlerProxy;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;


/**
 * 主启动类
 *
 * @author Happysnaker
 * @description
 * @date 2022/1/19
 * @email happysnaker@foxmail.com
 */
@Slf4j
public class HBotStarter {

    /**
     * JAVA 程序单独启动入口
     */
    public static void start() throws Exception {
        // 补丁
        Patch.patch();

        // 打印 banner
        HBotPrinter.printBanner();

        // 加载配置
        ConfigManager.loadConfig();

        // 执行用户初始化逻辑
        HBot.applicationContext.getBeanProvider(BotStartInitializer.class)
                .stream()
                .forEach(BotStartInitializer::init);

        // 登录机器人
        HBot.autoLogin();

        // 后台任务
        HBotCronJob.cronIfEnable();

        // 获取消息代理
        MessageHandlerProxy messageHandler = new MessageHandlerProxy();

        //订阅群聊消息事件
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, event -> {
            if (messageHandler.shouldHandle(event, null)) {
                messageHandler.handleMessageEvent(event, null);
            }
        });
    }
}

