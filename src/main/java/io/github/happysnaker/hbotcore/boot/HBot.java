package io.github.happysnaker.hbotcore.boot;

import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.IOUtil;
import io.github.happysnaker.hbotcore.utils.MapGetter;
import io.github.happysnaker.hbotcore.utils.StringUtil;
import lombok.extern.log4j.Log4j2;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.auth.BotAuthorization;
import net.mamoe.mirai.utils.BotConfiguration;
import net.mamoe.mirai.utils.MiraiLogger;
import net.mamoe.mirai.utils.PlatformLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;

/**
 * 此类定义了一些 Hbot 全局信息以及可能会使用的配置项<p>
 * 此类对外暴露 bot 登录的接口
 * </p>
 *
 * @Author happysnaker
 * @Date 2023/2/24
 * @Email happysnaker@foxmail.com
 */
@Log4j2
@Component("Hbot")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HBot {
    /**
     * 程序工作路径
     */
    public static String ROOT_DIR;
    /**
     * bot 目录，bot 目录下可能会存放多个 bot 的文件，以它们的 qq 号命名
     */
    public static String BOT_DIR;
    /**
     * 配置文件存放路径，主要包含两个配置文件，config.yaml 和 auto-login.yaml 文件
     */
    public static String CONFIG_DIR;
    /**
     * 运行时数据文件目录
     */
    public static String DATA_DIR;
    /**
     * HBOt 插件存放目录
     */
    public static String PLUGIN_DIR;
    /**
     * HRobot 使用的日志系统
     */
    public static MiraiLogger LOGGER;
    /**
     * Spring 工厂
     */
    public static ApplicationContext applicationContext;

    static {
        // default
        ROOT_DIR = System.getProperty("user.dir");
        BOT_DIR = joinPath(ROOT_DIR, "bots");
        CONFIG_DIR = joinPath(ROOT_DIR, "config");
        DATA_DIR = joinPath(ROOT_DIR, "data");
        PLUGIN_DIR = joinPath(ROOT_DIR, "plugins");
    }

    public HBot(
            @Value("${hrobot.path.rootDir:}") String rootDir,
            @Value("${hrobot.path.botDir:}") String botDir,
            @Value("${hrobot.path.dataDir:}") String dataDir,
            @Value("${hrobot.path.configDir:}") String configDir,
            @Value("${hrobot.path.pluginDir:}") String pluginDir) {
        setWorkDir(rootDir, botDir, dataDir, configDir, pluginDir);
    }


    /**
     * 设置程序工作目录
     *
     * @param rootDir   主目录
     * @param botDir    账号目录，此目录会内嵌在 rootDir，请勿在路径中包含 rootDir
     * @param dataDir   数据目录，此目录会内嵌在 rootDir，请勿在路径中包含 rootDir
     * @param configDir 配置目录，此目录会内嵌在 rootDir，请勿在路径中包含 rootDir
     * @param pluginDir 插件目录，此目录会内嵌在 rootDir，请勿在路径中包含 rootDir
     */
    public static void setWorkDir(String rootDir, String botDir, String dataDir, String configDir, String pluginDir) {
        if (!StringUtil.isNullOrEmpty(rootDir)) {
            ROOT_DIR = rootDir;
            File file = new File(ROOT_DIR);
            if (!file.exists()) {
                boolean b = file.mkdirs();
            }
            // flush
            BOT_DIR = joinPath(ROOT_DIR, "bots");
            CONFIG_DIR = joinPath(ROOT_DIR, "config");
            DATA_DIR = joinPath(ROOT_DIR, "data");
            PLUGIN_DIR = joinPath(ROOT_DIR, "plugins");
        }
        if (!StringUtil.isNullOrEmpty(botDir))
            BOT_DIR = joinPath(ROOT_DIR, botDir);
        if (!StringUtil.isNullOrEmpty(dataDir))
            DATA_DIR = joinPath(ROOT_DIR, dataDir);
        if (!StringUtil.isNullOrEmpty(configDir))
            CONFIG_DIR = joinPath(ROOT_DIR, configDir);
        if (!StringUtil.isNullOrEmpty(pluginDir))
            PLUGIN_DIR = joinPath(ROOT_DIR, pluginDir);

        // init
        LOGGER = MiraiLogger.Factory.INSTANCE.create(PlatformLogger.class);
        File config = new File(CONFIG_DIR);
        if (!config.exists()) {
            boolean b = config.mkdirs();
        }

        File autoLogin = new File(CONFIG_DIR, "auto-login.yaml");
        if (!autoLogin.exists()) {
            try {
                boolean b = autoLogin.createNewFile();
                var stream = HBot.class.getClassLoader()
                        .getResourceAsStream("auto-login.template.yaml");
                assert stream != null;
                IOUtil.writeToFile(autoLogin, new String(stream.readAllBytes()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        File data = new File(DATA_DIR);
        if (!data.exists()) {
            boolean b = data.mkdirs();
        }

        File plugins = new File(PLUGIN_DIR);
        if (!plugins.exists()) {
            boolean b = plugins.mkdirs();
        }
    }

    // set context
    @Autowired
    public void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    public static String joinPath(String p1, String p2) {
        return new File(p1, p2).getPath();
    }


    /**
     * 根据默认的配置项登录一个 bot(以 PHONE 登录)
     *
     * @param qq       账号
     * @param password 密码
     */
    public static void loginBot(long qq, String password) {
        loginBot(qq, password, BotConfiguration.MiraiProtocol.ANDROID_PHONE);
    }


    /**
     * 以特定的协议登录一个 bot
     *
     * @param qq       账号
     * @param password 密码
     */
    public static void loginBot(long qq, String password, BotConfiguration.MiraiProtocol protocol) {
        loginBot(qq, password, new BotConfiguration() {
            {
                setWorkingDir(new File(BOT_DIR, String.valueOf(qq)));
                fileBasedDeviceInfo();
                setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
                setProtocol(protocol);
                noNetworkLog();
                setHighwayUploadCoroutineCount(Runtime.getRuntime().availableProcessors() << 1);
            }
        });
    }

    /**
     * 扫码登录
     * @param qq
     * @param protocol 协议仅支持 macos 和 watch
     */
    public static void loginBotByQRCode(long qq, BotConfiguration.MiraiProtocol protocol) {
        loginBotByQRCode(qq, new BotConfiguration() {
            {
                setWorkingDir(new File(BOT_DIR, String.valueOf(qq)));
                fileBasedDeviceInfo();
                setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
                setProtocol(protocol);
                noNetworkLog();
                setHighwayUploadCoroutineCount(Runtime.getRuntime().availableProcessors() << 1);
            }
        });
    }


    /**
     * 扫码登录
     * @param qq
     * @param cfg
     */
    public static void loginBotByQRCode(long qq, BotConfiguration cfg) {
        File file = new File(BOT_DIR, String.valueOf(qq));
        if (!file.exists()) {
            boolean b = file.mkdirs();
        }
        if (cfg.getProtocol() != BotConfiguration.MiraiProtocol.MACOS &&
                cfg.getProtocol() != BotConfiguration.MiraiProtocol.ANDROID_WATCH) {
            throw new IllegalArgumentException("Using QRCode, protocol must be watch or macos, but actually is " + cfg.getProtocol());
        }
        cfg.setWorkingDir(new File(BOT_DIR, String.valueOf(qq)));
        BotFactory.INSTANCE.newBot(qq, BotAuthorization.byQRCode(), cfg).login();
    }

    /**
     * 登录一个 qq
     *
     * @param qq  账号
     * @param pwd 密码
     * @param cfg 配置
     */
    public static void loginBot(long qq, String pwd, BotConfiguration cfg) {
        File file = new File(BOT_DIR, String.valueOf(qq));
        if (!file.exists()) {
            boolean b = file.mkdirs();
        }
        cfg.setWorkingDir(new File(BOT_DIR, String.valueOf(qq)));
        BotFactory.INSTANCE.newBot(qq, pwd, cfg).login();
    }


    /**
     * 读取 config/auto-login.yaml 文件并进行自动登录
     */
    public static void autoLogin() {
        Yaml yaml = new Yaml();
        File file = new File(CONFIG_DIR, "auto-login.yaml");
        if (!file.exists()) {
            LOGGER.info("未检测到 HBot 自动登录文件，忽略相关动作");
            return;
        }
        MapGetter mg = new MapGetter((Object) yaml.load(IOUtil.readFile(file)));
        int count = 0;
        for (MapGetter account : mg.getMapGetterList("accounts")) {
            boolean byQRCode = account.getOrDefault("byQRCode", false, Boolean.class);
            MapGetter configuration = account.getMapGetter("configuration");
            if (!configuration.getBoolean("enable")) {
                continue;
            }
            long qq = account.getLong("account");
            String password = account.getString("password", true);
            String protocol = configuration.getString("protocol");
            BotConfiguration.MiraiProtocol miraiProtocol = switch (protocol) {
                case "ANDROID_PHONE" -> BotConfiguration.MiraiProtocol.ANDROID_PHONE;
                case "ANDROID_PAD" -> BotConfiguration.MiraiProtocol.ANDROID_PAD;
                case "ANDROID_WATCH" -> BotConfiguration.MiraiProtocol.ANDROID_WATCH;
                case "MACOS" -> BotConfiguration.MiraiProtocol.MACOS;
                default -> {
                    throw new RuntimeException("自动登录文件中 protocol 配置错误，请检查");
                }
            };
            BotConfiguration cfg = new BotConfiguration() {
                {
                    setWorkingDir(new File(BOT_DIR, String.valueOf(qq)));
                    fileBasedDeviceInfo();
                    setHeartbeatStrategy(HeartbeatStrategy.STAT_HB);
                    setProtocol(miraiProtocol);
                    noNetworkLog();
                    setHighwayUploadCoroutineCount(Runtime.getRuntime().availableProcessors() << 1);
                }
            };
            if (!byQRCode) {
                loginBot(qq, password, cfg);
            } else {
                Logger.info("Login by QRCode for %d, please make sure the protocol is watch or macos, " +
                        "current protocol is %s", qq, cfg.getProtocol().toString());
                loginBotByQRCode(qq, cfg);
            }
            count++;
        }
        Logger.info("已自动登录 %d 个账号", count);
    }
}
