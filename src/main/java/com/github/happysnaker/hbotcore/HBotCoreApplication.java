package com.github.happysnaker.hbotcore;

import com.github.happysnaker.hbotcore.boot.EnableHBot;
import com.github.happysnaker.hbotcore.boot.HBot;
import com.github.happysnaker.hbotcore.config.ConfigManager;
import com.github.happysnaker.hbotcore.config.HBotConfigComponent;
import com.github.happysnaker.hbotcore.gpt.AuthConfig;
import com.github.happysnaker.hbotcore.gpt.RevChatGptHandler;
import lombok.Data;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.auth.BotAuthorization;
import net.mamoe.mirai.utils.BotConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableHBot
public class HBotCoreApplication {
    public static final String token = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJoYXBweXNuYWtlckBmb3htYWlsLmNvbSIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlfSwiaHR0cHM6Ly9hcGkub3BlbmFpLmNvbS9hdXRoIjp7InVzZXJfaWQiOiJ1c2VyLTRwaXdid0p2UWZha1I0Rkgyd2didXFBWCJ9LCJpc3MiOiJodHRwczovL2F1dGgwLm9wZW5haS5jb20vIiwic3ViIjoiYXV0aDB8NjM5MDc1ZjZkNTBkYmVhZjQxZmE0ODE3IiwiYXVkIjpbImh0dHBzOi8vYXBpLm9wZW5haS5jb20vdjEiLCJodHRwczovL29wZW5haS5vcGVuYWkuYXV0aDBhcHAuY29tL3VzZXJpbmZvIl0sImlhdCI6MTY4MTYxOTU2NSwiZXhwIjoxNjgyODI5MTY1LCJhenAiOiJUZEpJY2JlMTZXb1RIdE45NW55eXdoNUU0eU9vNkl0RyIsInNjb3BlIjoib3BlbmlkIHByb2ZpbGUgZW1haWwgbW9kZWwucmVhZCBtb2RlbC5yZXF1ZXN0IG9yZ2FuaXphdGlvbi5yZWFkIG9mZmxpbmVfYWNjZXNzIn0.UXOJ_LcibG14DoinzkuSCsgea0QOKK6ObEjmEw0r2L63N4ZEj40EinGdFMcTj9aVhzyMsGENMsR1cw2PZ_t8h7b1FHAvG5N5kEsP9qNpVTZZOhCZ5nq4OVnhh0H9clM-OhL48n1d3SkJPQHbqgpWvl2U-lk9xtX0toyLgNoZQ0UDAJGQkqE6sGNb7a_DOmN9W8Q3IrHtZRfKGLncvGshNh6LEbncFugduoPywxGPqUqnbcPAGggZmcBpT7H7no2_Fb_1-WQWeCtwRlwx28JZAgGYSWryZ_XA8rIPezz3arr-59NUC_HbFtBCMv5br2vtHSPLNw_P4JF-qkzJh9Ny3w";

    public static void main(String[] args) throws Exception {
        RevChatGptHandler.updateAuthConfig(List.of(AuthConfig.builder().accessToken(token).build()));
        RevChatGptHandler.enable = true;
        SpringApplication.run(HBotCoreApplication.class);
        HBot.autoLogin();
    }
}


@Data
@HBotConfigComponent
class Config {
    public static List<String> config;
    public static Map<String, List<String>> m;
    public static Set<String> botSuperAdministrator = Set.of("1637318597");
    public static Set<String> botAdministrator = Set.of("1637318597");
    public static Set<String> botGroupAdministrator = Set.of("1637318597");
    public static String str;
}





