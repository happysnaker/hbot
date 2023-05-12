package com.github.happysnaker.hbotcore.handler;

import java.lang.annotation.*;

/**
 * 支持多个 {@link InterestFilter} 组合
 * @see InterestFilter
 * @Author happysnaker
 * @Date 2023/4/20
 * @Email happysnaker@foxmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Inherited
public @interface InterestFilters {
    InterestFilter[] value();
    boolean matchAll() default false;
}
