package io.github.happysnaker.hbotcore.permisson;

import io.github.happysnaker.hbotcore.config.ConfigManager;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import io.github.happysnaker.hbotcore.utils.MapGetter;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 默认的权限管理器，仅支持由 {@link PermissionManager} 提供的三种权限比较
 * <p>使用此权限管理器需要遵循 HBot 的约定，HBot 约定权限配置均以类变量的形式定义在配置类中，具体地请遵循如下定义
 * <ul>
 *     <li>超级管理员定义配置：public static String/List botSuperAdministrator</li>
 *     <li>普通管理员定义配置：public static String/List botAdministrator</li>
 *     <li>群内管理员定义配置：public static Map botGroupAdministrator</li>
 * </ul>
 * 如果未按标准定义，则会抛出运行时异常
 * </p>
 *
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@Service
@Order
public class DefaultPermissionManager implements PermissionManager {

    @Override
    public boolean hasPermission(int perm, GroupMessageEvent event, Context ctx) {
        return switch (perm) {
            case BOT_SUPER_ADMINISTRATOR -> hasSuperAdmin(HBotUtil.getSenderId(event));
            case BOT_ADMINISTRATOR -> hasAdmin(HBotUtil.getSenderId(event));
            case BOT_GROUP_ADMINISTRATOR ->
                    hasGroupAdmin(HBotUtil.getSenderId(event), String.valueOf(event.getGroup().getId()));
            default -> false;
        };
    }

    @Override
    public Set<Integer> getPermissionList(String user, String gid) {
        Set<Integer> perms = new HashSet<>();
        if (hasSuperAdmin(user))        perms.add(BOT_SUPER_ADMINISTRATOR);
        if (hasAdmin(user))             perms.add(BOT_ADMINISTRATOR);
        if (hasGroupAdmin(user, gid))   perms.add(BOT_GROUP_ADMINISTRATOR);
        return perms;
    }


    public boolean hasSuperAdmin(String sender) {
        Object config = ConfigManager.getConfig("botSuperAdministrator");
        if (config instanceof String && sender.equals(config)) {
            return true;
        }
        if (config instanceof Collection<?> coll && coll.iterator().hasNext()) {
            return coll.iterator().next().toString().equals(sender);
        }
        return config != null && config.toString().equals(sender);
    }

    public boolean hasAdmin(String sender) {
        if (hasSuperAdmin(sender)) {
            return true;
        }
        Object config = ConfigManager.getConfig("botAdministrator");
        if (config instanceof Collection<?> coll) {
            for (Object o : coll) {
                if (o != null && String.valueOf(o).equals(sender)) {
                    return true;
                }
            }
        }
        return config != null && config.toString().equals(sender);
    }

    public boolean hasGroupAdmin(String sender, String gid) {
        if (hasAdmin(sender)) {
            return true;
        }
        Object config = ConfigManager.getConfig("botGroupAdministrator");
        if (config instanceof Map<?, ?> map) {
            MapGetter mg = new MapGetter(map);
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = entry.getKey().toString();
                if (!key.equals(sender)) {
                    continue;
                }
                if (gid == null) {
                    return true;
                }
                List<Object> perms = mg.getListOrWrapperSingleton(key, Object.class);
                return perms.stream().anyMatch(o -> o.toString().equals(gid));
            }
        }
        return config != null && config.toString().equals(sender);
    }
}
