package com.github.happysnaker.hbotcore.config;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标记类成为 Hbot 指定的配置类
 * @Author happysnaker
 * @Date 2023/2/25
 * @Email happysnaker@foxmail.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component()
public @interface HBotConfigComponent {
}
