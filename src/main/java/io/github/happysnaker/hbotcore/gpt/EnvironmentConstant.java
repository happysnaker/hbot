package io.github.happysnaker.hbotcore.gpt;

import java.time.Duration;

public class EnvironmentConstant {
    public static final String HOME = "user.home";

    public static final String CONFIG_PATH = "/.config/revChatGPT/config.json";

    public static final String BASE_URL = "https://ai.fakeopen.com/api/conversation";

    public static final Duration TIMEOUT = Duration.ofSeconds(360);
}
