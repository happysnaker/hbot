package io.github.happysnaker.hbotcore;

import cn.hutool.log.Log;
import io.github.happysnaker.hbotcore.boot.EnableHBot;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.boot.HBotConfig;
import io.github.happysnaker.hbotcore.command.HBotCommandEventHandlerManager;
import io.github.happysnaker.hbotcore.config.ConfigManager;
import io.github.happysnaker.hbotcore.config.HBotConfigComponent;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.plugin.HBotPluginEntry;
import io.github.happysnaker.hbotcore.plugin.HBotPluginLoader;

import io.github.happysnaker.hbotcore.plugin.HBotPluginRegister;
import io.github.happysnaker.hbotcore.plugin.PluginClassLoader;
import lombok.Data;
import net.mamoe.mirai.utils.BotConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

@SpringBootApplication
@EnableHBot
public class HBotCoreApplication {
    static  Scanner scanner = new Scanner(System.in);;
    public static void main(String[] args) throws Exception {
        Thread.currentThread().setContextClassLoader(
                PluginClassLoader.getInstance(HBotCoreApplication.class));
        SpringApplication.run(HBotCoreApplication.class, args);
    }
}


@Data
class Config {
    public static List<User> config;
    public static Map<String, List<String>> m;
    public static Set<String> botSuperAdministrator = Set.of("1637318597");
    public static Set<String> botAdministrator = Set.of("1637318597");
    public static Map<String, List<String>> botGroupAdministrator = Map.of(
            "1637318597", List.of("9030252723"));
    public static String str;
}

@Data
class User {
    String qq;
    String pp;
}





