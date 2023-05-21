package io.github.happysnaker.hbotcore.boot;

import io.github.happysnaker.hbotcore.logger.Logger;
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


    public static final int STOP = 0;
    public static final int STARTING = 1;
    public static final int RUNNING = 2;

    public static int status = STOP;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        status = STARTING;
        long start = System.currentTimeMillis();
        HBotStarter.start();
        long end = System.currentTimeMillis();
        Logger.info("started hbot in %f seconds", (end - start) * 1.0 / 1000f);
        status = RUNNING;
    }
}
