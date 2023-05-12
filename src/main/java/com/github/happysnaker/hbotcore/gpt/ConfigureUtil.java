package com.github.happysnaker.hbotcore.gpt;



import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class ConfigureUtil {
    public static AuthConfig getConfigure() {
        String home = System.getProperty(EnvironmentConstant.HOME);
        String configPath = home + EnvironmentConstant.CONFIG_PATH;
        Path filePath = Paths.get(configPath);

        String content = null;

        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            System.err.println("No config file found.");
        }
        return JSONObject.parseObject(content, AuthConfig.class);
    }
}
