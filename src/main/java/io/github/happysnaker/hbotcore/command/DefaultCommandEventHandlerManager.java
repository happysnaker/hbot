package io.github.happysnaker.hbotcore.command;

import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.StringUtil;
import io.github.happysnaker.hbotcore.utils.IOUtil;
import lombok.SneakyThrows;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import org.springframework.beans.factory.annotation.Value;

import java.io.*;

/**
 * 此类是 Hbot 内置的一个默认的命令管理类，此类提供一些默认的行为：
 * <ul>
 *     <li>在此类会记录下管理员执行的所有命令，可调用 {@link #getCache()} 查看或 {@link #flush()} 重置</li>
 *     <li>此类支持根据用户配置截断命令缓存</li>
 *     <li>此类会将执行失败的命令写入错误日志</li>
 * </ul>
 */
public abstract class DefaultCommandEventHandlerManager extends AbstractCommandEventHandler implements Serializable {
    /**
     * 命令前缀
     */
    public static String prefix = "#";
    /**
     * 保存的最大字节数
     */
    public static int maxSaveBytes = 1024;
    /**
     * 错误日志文件
     */
    public static String errorLogFile = HBot.joinPath(HBot.DATA_DIR, "command_error.log");


    private static final StringBuffer cache = new StringBuffer();


    @Value("${hrobot.command.prefix:#}")
    protected void setPrefix(String prefix) {
        if (prefix != null)
            DefaultCommandEventHandlerManager.prefix = prefix;
    }
    @Value("${hrobot.command.maxSaveBytes:1024}")
    public void setMaxSaveBytes(Integer maxSaveBytes) {
        if (maxSaveBytes != null && maxSaveBytes >= 0)
            DefaultCommandEventHandlerManager.maxSaveBytes = maxSaveBytes;
    }
    @Value("${hrobot.command.errorLogFile:}")
    public void setErrorLogFile(String errorLogFile) {
        if (!StringUtil.isNullOrEmpty(errorLogFile))
            DefaultCommandEventHandlerManager.errorLogFile = HBot.joinPath(HBot.DATA_DIR, errorLogFile);
    }


    @Override
    public void success(GroupMessageEvent event) {
        String formatLog = Logger.formatLog(event);
        cache.append("[SUCCESS] ")
                .append(formatLog)
                .append('\n');
        if (cache.length() > maxSaveBytes) {
            cache.replace(0, cache.length() - maxSaveBytes, "");
        }
    }

    @SneakyThrows
    @Override
    public void fail(GroupMessageEvent event, String errorMsg) {
        String formatLog = Logger.formatLog(event);
        StringBuilder sb = new StringBuilder();
        sb.append("[SUCCESS] ").append(formatLog).append('\n');
        cache.append(sb.toString());
        if (cache.length() > maxSaveBytes) {
            cache.replace(0, cache.length() - maxSaveBytes, "");
        }
        Logger.error(errorMsg);

        sb.append("出错原因 ==>").append(errorMsg);


        Logger.FILE_WRITER_EXECUTOR.submit(() -> {
            try {
                IOUtil.writeToFile(new File(errorLogFile), sb.toString(), true);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 刷新命令执行缓存记录
     */
    public static void flush() {
        cache.delete(0, cache.length());
    }

    /**
     * 获取命令执行缓存记录
     */
    public static String getCache() {
        return cache.toString();
    }
}
