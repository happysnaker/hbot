package io.github.happysnaker.hbotcore.plugin;

import io.github.happysnaker.hbotcore.boot.BotStartInitializer;
import io.github.happysnaker.hbotcore.config.ConfigListener;
import io.github.happysnaker.hbotcore.config.ConfigManager;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.intercept.Intercept;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.utils.Pair;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.loader.LaunchedURLClassLoader;

import java.io.File;
import java.net.URLClassLoader;
import java.util.List;
import java.util.jar.JarFile;

/**
 * 插件信息
 * @Author happysnaker
 * @Date 2023/5/15
 * @Email happysnaker@foxmail.com
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HBotPluginEntry {
    private JarFile jarFile;
    private String name;
    private Class<? extends HBotPlugin> pluginClass;
    private Class<?> configClass;
    private URLClassLoader classLoader;
    private List<Pair<String, String>> customerClass;
    private List<Class<? extends MessageEventHandler>> handlerList;
    private List<Class<? extends Interceptor>> interceptorList;
    private List<Class<? extends ConfigListener>> configListenerList;
    private List<Class<? extends BotStartInitializer>> initializerList;
    private List<Class<?>> allClass;
}
