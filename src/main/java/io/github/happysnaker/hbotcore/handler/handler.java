package io.github.happysnaker.hbotcore.handler;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 标记该类成为以一个消息事件处理者
 * @author happysnakers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Component
public @interface handler {
    /**
     * 处理消息的优先级，如果有多个处理者都对消息感兴趣，那么优先级最高的处理者将优先处理
     */
    int priority() default 1;

    /**
     * 是否为一个命令处理器
     */
    boolean isCommandHandler() default false;
}
