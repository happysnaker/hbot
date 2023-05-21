package io.github.happysnaker.hbotcore.boot;

import io.github.happysnaker.hbotcore.command.HBotCommandEventHandlerManager;
import io.github.happysnaker.hbotcore.cron.HBotCronJob;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.IOUtil;
import io.github.happysnaker.hbotcore.utils.StringUtil;
import jakarta.annotation.PostConstruct;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.utils.MiraiLogger;
import net.mamoe.mirai.utils.PlatformLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static io.github.happysnaker.hbotcore.boot.HBot.*;
import static io.github.happysnaker.hbotcore.logger.Logger.logFile;

/**
 * @Author happysnaker
 * @Date 2023/4/12
 * @Email happysnaker@foxmail.com
 */
@Configuration
@ComponentScan("io.github.happysnaker.hbotcore")
public class HBotConfig {
    final static HBotConfig self = new HBotConfig();

    public static void setCommandPrefix0(String prefix) {
        self.setCommandPrefix(prefix);
    }

    @Value("${hrobot.command.prefix:#}")
    public void setCommandPrefix(String prefix) {
        if (prefix != null)
            HBotCommandEventHandlerManager.prefix = prefix;
    }

    public static void setCommandMaxSaveBytes0(Integer maxSaveBytes) {
        self.setCommandMaxSaveBytes(maxSaveBytes);
    }

    @Value("${hrobot.command.maxSaveBytes:1024}")
    public void setCommandMaxSaveBytes(Integer maxSaveBytes) {
        if (maxSaveBytes != null && maxSaveBytes >= 0)
            HBotCommandEventHandlerManager.maxSaveBytes = maxSaveBytes;
    }


    public static void setErrorLogFile0(String errorLogFile) {
        self.setErrorLogFile(errorLogFile);
    }

    @Value("${hrobot.command.errorLogFile:}")
    public void setErrorLogFile(String errorLogFile) {
        if (!StringUtil.isNullOrEmpty(errorLogFile))
            HBotCommandEventHandlerManager.errorLogFile = HBot.joinPath(HBot.DATA_DIR, errorLogFile);
    }

    public static void setLogLevel0(String level) {
        self.setLogLevel(level);
    }

    @Value("${hrobot.logging.level:debug}")
    public void setLogLevel(String level) {
        try {
            Logger.logLevel = Integer.parseInt(level);
        } catch (Exception ignore) {
            Logger.logLevel = switch (level.toLowerCase(Locale.ROOT).trim()) {
                case "debug" -> Logger.DEBUG;
                case "info" -> Logger.INFO;
                case "warning" -> Logger.WARNING;
                case "error" -> Logger.ERROR;
                default -> throw new IllegalStateException("Unexpected logLevel: " + level);
            };
        }
        if (Logger.logLevel != Logger.DEBUG
                && Logger.logLevel != Logger.INFO
                && Logger.logLevel != Logger.WARNING
                && Logger.logLevel != Logger.ERROR) {
            throw new IllegalStateException("Unexpected logLevel: " + level);
        }
    }

    public static void setToFileMod0(int mod) {
        self.setToFileMod(mod);
    }

    @Value("${hrobot.logging.toFileMod:0}")
    public void setToFileMod(int mod) {
        Logger.toFileMod = switch (mod) {
            case 0 -> 0;
            case 1 -> Logger.WARNING_ERROR_TO_FILE;
            case 2 -> Logger.ALL_TO_FILE;
            default -> throw new IllegalStateException("Unexpected toFileMod: " + mod);
        };
    }


    public static void setToFilePath0(String path) {
        self.setToFilePath(path);
    }


    @Value("${hrobot.logging.filePath:}")
    public void setToFilePath(String path) {
        if (path != null && !path.isEmpty()) {
            Logger.logFile = path;
            if (!path.contains(HBot.DATA_DIR)) {
                Logger.logFile = HBot.joinPath(HBot.DATA_DIR, Logger.logFile);
            }
        }
        if (Logger.logFile.endsWith(".log")) {
            Logger.logFile = Logger.logFile.replace(".log", "");
        }
    }

    public static void setLogMaxFileSize0(String size) {
        self.setLogMaxFileSize(size);
    }


    @Value("${hrobot.logging.maxSize:10mb}")
    public void setLogMaxFileSize(String size) {
        size = size.toLowerCase(Locale.ROOT).trim();
        try {
            if (size.endsWith("gb")) {
                Logger.fileSizeHolder = 1024 * 1024 * 1024 * Integer.parseInt(size.replace("gb", ""));
            } else if (size.endsWith("mb")) {
                Logger.fileSizeHolder = 1024 * 1024 * Integer.parseInt(size.replace("mb", ""));
            } else if (size.endsWith("kb")) {
                Logger.fileSizeHolder = 1024 * Integer.parseInt(size.replace("kb", ""));
            } else {
                throw new RuntimeException();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected size holder: " + size);
        }
    }


    public static void setPeriodMinute0(int periodMinute) {
        self.setPeriodMinute(periodMinute);
    }

    @Value("${hrobot.cron.periodMinute:3}")
    public void setPeriodMinute(int periodMinute) {
        HBotCronJob.PERIOD_MINUTE = periodMinute;
    }


    public static void setPeriodEnable0(boolean enable) {
        self.setPeriodEnable(enable);
    }

    @Value("${hrobot.cron.enable:true}")
    public void setPeriodEnable(boolean enable) {
        HBotCronJob.enable = enable;
    }
}
