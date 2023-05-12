package com.github.happysnaker.hbotcore.logger;


import com.github.happysnaker.hbotcore.boot.HBot;
import com.github.happysnaker.hbotcore.utils.StringUtil;
import com.github.happysnaker.hbotcore.utils.IOUtil;
import com.github.happysnaker.hbotcore.utils.HBotUtil;
import lombok.SneakyThrows;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.happysnaker.hbotcore.utils.HBotUtil.getContent;

/**
 * <P>HRobot 提供一个简单的日志记录器，本质上调用了 {@link net.mamoe.mirai.utils.MiraiLogger} 进行日志输出，但 HRobot 对用户开放了日志级别的设定。</P>
 * <p><strong>如果用户不想使用 {@link net.mamoe.mirai.utils.MiraiLogger}，可修改 {@link HBot#LOGGER} 对象，此类依赖与此对象进行日志输出</strong></p>
 * <p>HRobot 中日志级别为：DEBUG < INFO < WARNING < ERROR，可以在配置文件中或直接修改此类的静态配置属性 {@link #logLevel}设定输出级别，低于设定级别的日志不会输出</p>
 * <p>HRobot 默认情况下仅会在控制台输出，但是可以通过设置一些属性或者通过配置文件，使得 HRobot 将日志同步写入文件，并可以指定文件的最大阈值，HRobot 支持仅写入警告级别以上的日志与写入所有日志两种配置</p>
 *
 * @Author happysnaker
 * @Date 2023/2/15
 * @Email happysnaker@foxmail.com
 */
@Component
@DependsOn("Hbot")
public class Logger {
    // 日志级别
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;

    // 输出文件模式
    public static final int ALL_TO_FILE = 9; // 所有日志输出都保存到文件
    public static final int WARNING_ERROR_TO_FILE = 10; // 仅警告级别以上日志输出都保存到文件


    // 用户配置
    public static int logLevel = 0; // default debug
    public static int toFileMod = 0; // default disable，TO_FILE 或 ERROR_TO_FILE 两种模式之一，其他情况下则禁用此功能
    public static String logFile = HBot.joinPath(HBot.DATA_DIR, "run.log"); // default data/run.log
    public static int fileSizeHolder = 1024 * 1024 * 10; // 文件大小阈值，以字节为单位

    /**
     * 文件单线程写入
     */
    public static final ExecutorService FILE_WRITER_EXECUTOR = Executors.newFixedThreadPool(1);

    @Value("${hrobot.logging.level:debug}")
    public void readLogLevel(String level) {
        try {
            logLevel = Integer.parseInt(level);
        } catch (Exception ignore) {
            logLevel = switch (level.toLowerCase(Locale.ROOT).trim()) {
                case "debug" -> DEBUG;
                case "info" -> INFO;
                case "warning" -> WARNING;
                case "error" -> ERROR;
                default -> throw new IllegalStateException("Unexpected logLevel: " + level);
            };
        }
        if (logLevel != DEBUG && logLevel != INFO && logLevel != WARNING && logLevel != ERROR) {
            throw new IllegalStateException("Unexpected logLevel: " + level);
        }
    }

    @Value("${hrobot.logging.toFileMod:0}")
    public void readToFileMod(int mod) {
        toFileMod = switch (mod) {
            case 0 -> 0;
            case 1 -> WARNING_ERROR_TO_FILE;
            case 2 -> ALL_TO_FILE;
            default -> throw new IllegalStateException("Unexpected toFileMod: " + mod);
        };
    }


    @Value("${hrobot.logging.filePath:}")
    public void readToFilePath(String path) {
        if (path != null && !path.isEmpty()) {
            logFile = path;
            if (!path.contains(HBot.DATA_DIR)) {
                logFile = HBot.joinPath(HBot.DATA_DIR, logFile);
            }
        }
        if (logFile.endsWith(".log")) {
            logFile = logFile.replace(".log", "");
        }
    }

    @Value("${hrobot.logging.maxSize:10mb}")
    public void readMaxFileSize(String size) {
        size = size.toLowerCase(Locale.ROOT).trim();
        try {
            if (size.endsWith("gb")) {
                fileSizeHolder = 1024 * 1024 * 1024 * Integer.parseInt(size.replace("gb", ""));
            } else if (size.endsWith("mb")) {
                fileSizeHolder = 1024 * 1024 * Integer.parseInt(size.replace("mb", ""));
            } else if (size.endsWith("kb")) {
                fileSizeHolder = 1024 * Integer.parseInt(size.replace("kb", ""));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected size holder: " + size);
        }
    }

    @SneakyThrows
    private static void toFile(String log, String level) {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        for (StackTraceElement e : stack) {
            if (!e.getClassName().equals(Logger.class.getName()) && !e.getClassName().equals(Thread.class.getName())) {
                StringBuilder className = new StringBuilder(e.getClassName());
                String[] split = className.toString().split("\\.");
                if (split.length >= 3) {
                    className = new StringBuilder();
                    for (int i = 0; i < split.length; i++) {
                        if (i <= 1)
                            className.append(split[i].charAt(0));
                        else
                            className.append(split[i]);

                        if (i != split.length - 1)
                            className.append('.');
                    }
                }
                log = String.format("%s  %7s ---[%15s] %-40s : %s\n", formatTime(), level, e.getMethodName(), className, log);
                break;
            }
        }
        File file;
        byte[] bytes = log.getBytes(StandardCharsets.UTF_8);
        int order = 0;
        do {
            file = order == 0 ? new File(logFile + ".log") : new File(String.format("%s.%d.log", logFile, order));
            if (!file.exists()) {
                boolean b = file.createNewFile();
            }
            order++;
        } while (!file.exists() || (file.length() != 0 && file.length() + bytes.length >= fileSizeHolder));

        String finalLog = log;
        File finalFile = file;
        FILE_WRITER_EXECUTOR.submit(() -> {
            try {
                IOUtil.writeToFile(finalFile, finalLog, true);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void debug(Throwable log) {
        debug(StringUtil.getErrorInfoFromException(log));
    }

    public static void debug(String log) {
        if (DEBUG >= logLevel) {
            HBot.LOGGER.debug(log);
            if (toFileMod == ALL_TO_FILE) {
                toFile(log, "DEBUG");
            }
        }
    }

    public static void debug(String format, Object... args) {
        debug(String.format(format, args));
    }


    public static void info(String log) {
        if (INFO >= logLevel) {
            HBot.LOGGER.info(log);
            if (toFileMod == ALL_TO_FILE) {
                toFile(log, "INFO");
            }
        }
    }

    public static void info(String format, Object... args) {
        info(String.format(format, args));
    }

    public static void info(Throwable log) {
        info(StringUtil.getErrorInfoFromException(log));
    }

    public static void warning(String log) {
        if (WARNING >= logLevel) {
            HBot.LOGGER.warning(log);
            if (toFileMod == ALL_TO_FILE || toFileMod == WARNING_ERROR_TO_FILE) {
                toFile(log, "WARNING");
            }
        }
    }

    public static void warning(String format, Object... args) {
        warning(String.format(format, args));
    }

    public static void warning(Throwable log) {
        warning(StringUtil.getErrorInfoFromException(log));
    }

    public static void error(String log) {
        if (ERROR >= logLevel) {
            HBot.LOGGER.error(log);
            if (toFileMod == ALL_TO_FILE || toFileMod == WARNING_ERROR_TO_FILE) {
                toFile(log, "ERROR");
            }
        }
    }


    public static void error(String format, Object... args) {
        error(String.format(format, args));
    }


    public static void error(Throwable log) {
        error(StringUtil.getErrorInfoFromException(log));
    }

    /**
     * 一种可能的日志格式化方式，记录下事件发生的场景
     *
     * @param event 事件
     * @return 格式化后的日志
     */
    public static String formatLog(MessageEvent event) {
        if (event == null) return "[" + formatTime() + "]";
        String content = HBotUtil.getContent(event);
        String sender = HBotUtil.getSenderId(event);
        if (!(event instanceof GroupMessageEvent)) {
            return "[sender:" + sender + "-" + formatTime() + "] -> " + content;
        }
        long groupId = ((GroupMessageEvent) event).getGroup().getId();
        return "[sender:" + sender + " - group:" + groupId + " - " + formatTime() + "] -> " + content;
    }


    private static String formatTime() {
        return formatTime(System.currentTimeMillis());
    }

    private static String formatTime(long ts) {
        Date d = new Date(ts);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(d);
    }
}
