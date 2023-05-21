package io.github.happysnaker.hbotcore.permisson;

import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.handler.AdaptInterestMessageEventHandler;
import io.github.happysnaker.hbotcore.handler.Interest;
import io.github.happysnaker.hbotcore.handler.InterestFilter;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.Pair;
import lombok.Data;
import lombok.SneakyThrows;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 权限管理 AOP
 *
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@Component
@Aspect
public class PermissionProxy {
    @Autowired
    private PermissionManager pm;


    private Object permissionFilter(ProceedingJoinPoint jp, Permission permission) throws Throwable {
        Pair<MessageEvent, Context> args = getArgs(jp.getArgs());
        if (args.getKey() == null) {
            return jp.proceed();  // not need
        }
        if (!pm.hasPermission(permission.value(), (GroupMessageEvent) args.getKey(), args.getValue())) {
            throw new InsufficientPermissionsException("权限不足");
        }
        return jp.proceed();
    }


    @Around("@within(io.github.happysnaker.hbotcore.permisson.Permission) && within(io.github.happysnaker.hbotcore.handler.MessageEventHandler+)")
    public Object permissionClassFilter(ProceedingJoinPoint jp) throws Throwable {
        Object target = jp.getTarget();
        if (jp.getSignature().getName().equals("handleMessageEvent") ||
                jp.getSignature().getName().equals("parseCommand")) {
            Permission permission = target.getClass().getAnnotation(Permission.class);
            return this.permissionFilter(jp, permission);
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
            if (arg instanceof Context c) {
                ctx = c;
            }
        }
        return Pair.of(e, ctx);
    }
}


