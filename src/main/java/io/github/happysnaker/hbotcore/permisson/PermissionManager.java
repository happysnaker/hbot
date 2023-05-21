package io.github.happysnaker.hbotcore.permisson;

import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.util.Set;

/**
 * 此接口是 HBot 中的权限管理器，HBot 内置了三种权限，分别是超级管理员、普通管理员、群管理员，权限依次下降，前者权限包含后者，群管理员只在对应群内拥有权限<p>
 *     通过 {@link Permission} 注解注释的方法最终会调用此接口进行条件判断，可实现此接口来定制化权限系统，若无实现，则会自动注入 HBot 内置的实现 {@link DefaultPermissionManager}
 * </p>
 * <strong>HBot 的管理员不等同于 QQ 群管理员，这两者毫无关系</strong>
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
public interface PermissionManager {
    /**
     * 超级管理员，至高无上的权限，最多只允许有 1 位
     */
    int BOT_SUPER_ADMINISTRATOR= 0;
    /**
     * 管理员，仅次于超级管理员，可以有无限多位
     */
    int BOT_ADMINISTRATOR = 1;
    /**
     * Bot 指定的群管理员，用于管理相关群，权限次于管理员与超级管理员
     */
    int BOT_GROUP_ADMINISTRATOR = 2;


    /**
     * 检测是否具有某权限
     * @param perm 待检测的权限
     * @param event 时间
     * @return 是否具有某权限
     */
    default boolean hasPermission(int perm, GroupMessageEvent event) {
        return hasPermission(perm, event, null);
    }


    /**
     * 检测是否具有某权限
     * @param perm 待检测的权限
     * @param event 时间
     * @param ctx 上下文
     * @return 是否具有某权限
     */
    boolean hasPermission(int perm, GroupMessageEvent event, Context ctx);

    /**
     * 返回某用户在某个群的权限列表
     * @param user 用户
     * @param gid 群号，可置空，如果置空则如果 user 在任意群拥有群管理员权限，此方法返回都应当包含群管理员，否则，如果 user 是群管理员但是无此群的权限，返回结果将不会包含群管理员
     * @return 权限列表
     */
    default Set<Integer> getPermissionList(String user, String gid) {
        throw new RuntimeException("未实现的方法");
    }


    /**
     * 返回用户的权限列表
     * @param user 用户
     * @return 权限列表
     */
    default Set<Integer> getPermissionList(String user) {
        return getPermissionList(user, null);
    }


    /**
     * 在某个群内比较两个用户的权限
     * @param u1 user1
     * @param u2 user2
     * @param gid 群，可置空
     * @return 如果 u1 > u2，返回 1，等于返回 0，小于返回 -1
     */
    default int compare(String u1, String u2, String gid) {
        Set<Integer> s1 = getPermissionList(u1, gid);
        Set<Integer> s2 = getPermissionList(u2, gid);
        if (s1.contains(BOT_SUPER_ADMINISTRATOR) || s2.contains(BOT_SUPER_ADMINISTRATOR)) {
            if (s1.contains(BOT_SUPER_ADMINISTRATOR) && s2.contains(BOT_SUPER_ADMINISTRATOR)) {
                return 0;
            }
            return s1.contains(BOT_SUPER_ADMINISTRATOR) ? 1 : -1;
        }

        if (s1.contains(BOT_ADMINISTRATOR) || s2.contains(BOT_ADMINISTRATOR)) {
            if (s1.contains(BOT_ADMINISTRATOR) && s2.contains(BOT_ADMINISTRATOR)) {
                return 0;
            }
            return s1.contains(BOT_ADMINISTRATOR) ? 1 : -1;
        }

        if (s1.contains(BOT_GROUP_ADMINISTRATOR) || s2.contains(BOT_GROUP_ADMINISTRATOR)) {
            if (s1.contains(BOT_GROUP_ADMINISTRATOR) && s2.contains(BOT_GROUP_ADMINISTRATOR)) {
                return 0;
            }
            return s1.contains(BOT_GROUP_ADMINISTRATOR) ? 1 : -1;
        }
        return 0;
    }
}
