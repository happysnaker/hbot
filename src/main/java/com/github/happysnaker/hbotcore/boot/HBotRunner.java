package com.github.happysnaker.hbotcore.boot;

import com.github.happysnaker.hbotcore.logger.Logger;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @Author happysnaker
 * @Date 2023/4/9
 * @Email happysnaker@foxmail.com
 */
@Component
public class HBotRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        long start = System.currentTimeMillis();
        HBotStarter.start();
        long end = System.currentTimeMillis();

        Logger.info("started hbot in %f seconds", (end - start) * 1.0 / 1000f);
    }
}
