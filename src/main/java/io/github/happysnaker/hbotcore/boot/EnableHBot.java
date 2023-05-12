package io.github.happysnaker.hbotcore.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @Author happysnaker
 * @Date 2023/4/9
 * @Email happysnaker@foxmail.com
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(HBotConfig.class)
public @interface EnableHBot {
}
