package io.github.happysnaker.hbotcore;

import io.github.happysnaker.hbotcore.boot.EnableHBot;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.config.HBotConfigComponent;
import io.github.happysnaker.hbotcore.gpt.AuthConfig;
import io.github.happysnaker.hbotcore.gpt.RevChatGptHandler;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableHBot
public class HBotCoreApplication {
    public static final String token = "";

    public static void main(String[] args) throws Exception {
        RevChatGptHandler.updateAuthConfig(List.of(AuthConfig.builder().accessToken(token).build()));
        RevChatGptHandler.enable = true;
        SpringApplication.run(HBotCoreApplication.class);
        HBot.autoLogin();
    }
}


@Data
class Config {
    public static List<String> config;
    public static Map<String, List<String>> m;
    public static Set<String> botSuperAdministrator = Set.of("1637318597");
    public static Set<String> botAdministrator = Set.of("1637318597");
    public static Set<String> botGroupAdministrator = Set.of("1637318597");
    public static String str;
}





