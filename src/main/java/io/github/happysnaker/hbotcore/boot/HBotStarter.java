package io.github.happysnaker.hbotcore.boot;


import io.github.happysnaker.hbotcore.config.ConfigManager;
import io.github.happysnaker.hbotcore.cron.HBotCronJob;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.plugin.HBotPluginLoader;

import io.github.happysnaker.hbotcore.plugin.HBotPluginRegister;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.event.GlobalEventChannel;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.io.File;
import java.util.Objects;


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

        // 扫描插件并加载
        loadAndRegistryJars();

        // 加载配置（连同插件配置）
        ConfigManager.loadConfig();

        // 在登录之前执行初始化逻辑
        HBot.applicationContext.getBeanProvider(BotStartInitializer.class)
                .stream()
                .forEach(BotStartInitializer::init);


        // 后台任务
        HBotCronJob.cronIfEnable();

        // 登录机器人被放在最后执行
        HBot.autoLogin();

        // 最后，订阅群聊消息事件
        GlobalEventChannel.INSTANCE.subscribeAlways(GroupMessageEvent.class, event -> {
            ((MessageEventHandler) HBot.applicationContext.getBean("proxyHandler")).handleMessageEvent(event, null);
        });
    }


    @SneakyThrows
    public static void loadAndRegistryJars() {
        File dirs = new File(HBot.PLUGIN_DIR);
        if (dirs.isDirectory()) {
            for (File file : Objects.requireNonNull(dirs.listFiles())) {
                if (file.getName().endsWith(".jar")) {
                    Logger.info("Start load and registry plugin file " + file.getName());
                    HBotPluginRegister.register(HBotPluginLoader.load(file));
                }
            }
        }
    }
}

