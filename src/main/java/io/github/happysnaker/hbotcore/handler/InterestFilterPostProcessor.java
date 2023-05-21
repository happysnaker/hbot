package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
//@Component
@Component
public class InterestFilterPostProcessor implements BeanPostProcessor {
    @Override
    @SneakyThrows
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(InterestFilter.class)) {
            injectInterestFilter(bean);
        }
        if (bean.getClass().isAnnotationPresent(InterestFilters.class)) {
            injectInterestFilters(bean);
        }
        return bean;
    }


    public static void injectInterestFilter(Object target) throws Throwable {
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
    }

    public static void injectInterestFilters(Object target) throws Throwable {
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
    }
}
