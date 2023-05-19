package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import io.github.happysnaker.hbotcore.proxy.Context;
import com.mchange.util.AssertException;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
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


    @Around("@within(InterestFilter)")
    public Object interestFilter(ProceedingJoinPoint jp) throws Throwable {
        Object target = jp.getTarget();
        InterestFilter filter = target.getClass().getAnnotation(InterestFilter.class);
        Interest interest = Interest.builder()
                .onCondition(filter)
                .builder();
        if (target instanceof AdaptInterestMessageEventHandler handler) {
            handler.setInterest(interest);
        } else if (target instanceof AdaptInterestCommandEventHandler handler) {
            handler.setInterest(interest);
        } else {
            throw new IllegalStateException("InterestFilters only can annotation on ");
        }
        return jp.proceed();
    }

    @Around("@within(InterestFilters)")
    public Object interestFilters(ProceedingJoinPoint jp) throws Throwable {
        Object target = jp.getTarget();
        InterestFilters filters = target.getClass().getAnnotation(InterestFilters.class);
        Interest.InterestBuilder builder = Interest.builder();
        for (InterestFilter filter : filters.value()) {
            builder.onCondition(filter);
        }
        if (!filters.matchAllCallbackMethod().isEmpty()) {
            builder.matchAll(filters.matchAll(), filters.matchAllCallbackMethod(), true);
        } else if (!filters.matchAllOutput().isEmpty()) {
            builder.matchAll(filters.matchAll(), filters.matchAllOutput(), false);
        } else {
            builder.matchAll(filters.matchAll());
        }
        if (target instanceof AdaptInterestMessageEventHandler handler) {
            handler.setInterest(builder.builder());
        } else if (target instanceof AdaptInterestCommandEventHandler handler) {
            handler.setInterest(builder.builder());
        } else {
            throw new IllegalStateException("InterestFilters only can annotation on ");
        }
        return jp.proceed();
    }
}
