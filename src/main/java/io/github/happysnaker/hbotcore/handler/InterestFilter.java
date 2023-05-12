package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.lang.annotation.*;

/**
 * {@link Interest} 的注解形式，允许注释在 {@link MessageEventHandler#shouldHandle(GroupMessageEvent, Context)} 方法上，但这种情况下不会进行分发，{@link #callbackMethod()} 属性是无效的。
 * <p>此注解允许注释在 {@link AdaptInterestCommandEventHandler} 或
 * {@link AdaptInterestMessageEventHandler} 类上，这种情况下 {@link #callbackMethod()} 属性有效，代理将会进行分发</p>
 * @Author happysnaker
 * @Date 2023/4/20
 * @Email happysnaker@foxmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface InterestFilter {
    String condition();
    Interest.MODE mode();
    String callbackMethod() default "3.141592653"; // 3.141592653 means disable
}
