package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

/**
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@Component
public class InterestFilterPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Interest.InterestBuilder builder = Interest.builder();
        if (bean instanceof AdaptInterestCommandEventHandler handler) {
            try {
                InterestFilter filter = bean.getClass().getAnnotation(InterestFilter.class);
                handler.setInterest(builder.onCondition(filter).builder());
            } catch (NullPointerException e) {
                try {
                    InterestFilters filters = bean.getClass().getAnnotation(InterestFilters.class);
                    for (InterestFilter filter : filters.value()) {
                        builder.onCondition(filter);
                    }
                    handler.setInterest(builder.matchAll(filters.matchAll()).builder());
                } catch (NullPointerException ignore) {
                    // ignore
                }
            }
        } else if (bean instanceof AdaptInterestMessageEventHandler handler) {
            try {
                InterestFilter filter = bean.getClass().getAnnotation(InterestFilter.class);
                handler.setInterest(builder.onCondition(filter).builder());
            } catch (NullPointerException e) {
                try {
                    InterestFilters filters = bean.getClass().getAnnotation(InterestFilters.class);
                    for (InterestFilter filter : filters.value()) {
                        builder.onCondition(filter);
                    }
                    handler.setInterest(builder.matchAll(filters.matchAll()).builder());
                } catch (NullPointerException ignore) {
                    // ignore
                }
            }
        }
        return bean;
    }
}
