package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.lang.annotation.*;

/**
 * <p>此注解允许注释在 {@link AdaptInterestCommandEventHandler} 或
 * {@link AdaptInterestMessageEventHandler} 类上，等同于向 super 注入 {@link Interest}</p>
 * @Author happysnaker
 * @Date 2023/4/20
 * @Email happysnaker@foxmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface InterestFilter {
    String condition();
    Interest.MODE mode();
    String callbackMethod() default "";
    String output() default "";
}
