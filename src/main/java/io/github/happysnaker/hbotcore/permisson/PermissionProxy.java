package io.github.happysnaker.hbotcore.permisson;

import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.handler.Interest;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.Pair;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@Component
@Aspect
public class PermissionProxy {
    @Autowired
    private PermissionManager pm;

    @Around("@annotation(permission)")
    public Object permissionFilter(ProceedingJoinPoint jp, Permission permission) throws Throwable {
        Pair<MessageEvent, Context> args = getArgs(jp.getArgs());
        if (args.getKey() == null) {
            return jp.proceed();  // not need
        }
        if (!pm.hasPermission(permission.value(), (GroupMessageEvent) args.getKey(), args.getValue())) {
            throw new InsufficientPermissionsException("权限不足");
        }
        return jp.proceed();
    }


    public static Pair<MessageEvent, Context> getArgs(Object[] args) {
        MessageEvent e = null;
        Context ctx = null;
        for (Object arg : args) {
            if (arg instanceof MessageEvent ev) {
                e = ev;
            }
            if (arg instanceof Interest.DispatchArgs disArgs) {
                e = disArgs.getEvent();
            }
            if (arg instanceof  Context c) {
                ctx = c;
            }
        }
        return Pair.of(e, ctx);
    }
}


