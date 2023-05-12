package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.proxy.Context;
import com.mchange.util.AssertException;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @Author happysnaker
 * @Date 2023/4/20
 * @Email happysnaker@foxmail.com
 */
@Aspect
@Component
public class InterestFilterProxy {

    public static void assertTargetMethod(ProceedingJoinPoint jp) {
        String methodName = jp.getSignature().getName(); // 获取被代理的方法名
        if (!methodName.equals("shouldHandle")) {
            throw new AssertException("InterestFilter can not annotation on method " + methodName);
        }
        Object[] args = jp.getArgs();
        if (args[0] != null && !(args[0] instanceof GroupMessageEvent)) {
            throw new AssertException("InterestFilter annotation error, first args not match" + args[0]);
        }
        if (args[1] != null && !(args[1] instanceof Context)) {
            throw new AssertException("InterestFilter annotation error, second args not match" + args[1]);
        }
    }

    @Around("@annotation(filter)")
    public Object interestFilter(ProceedingJoinPoint joinPoint, InterestFilter filter) throws Throwable {
        assertTargetMethod(joinPoint);
        return Interest.builder()
                .onCondition(filter)
                .builder()
                .isInterest((GroupMessageEvent) joinPoint.getArgs()[0]);
    }

    @Around("@annotation(filters)")
    public Object interestFilters(ProceedingJoinPoint joinPoint, InterestFilters filters) throws Throwable {
        // 这里编写拦截后的处理逻辑
        Interest.InterestBuilder builder = Interest.builder();
        for (InterestFilter filter : filters.value()) {
            builder.onCondition(filter);
        }
        builder.matchAll(filters.matchAll());
        return builder.builder().isInterest((GroupMessageEvent) joinPoint.getArgs()[0]);
    }
}
