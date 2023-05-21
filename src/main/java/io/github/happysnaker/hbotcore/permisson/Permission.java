package io.github.happysnaker.hbotcore.permisson;

import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;

import java.lang.annotation.*;

/**
 * 注释在方法上，标识方法必须需要某个权限方能调用
 * <p>允许标注在类上，这会代理 {@link io.github.happysnaker.hbotcore.handler.MessageEventHandler#handleMessageEvent(GroupMessageEvent, Context)}
 * 方法和 {@link io.github.happysnaker.hbotcore.command.CommandHandler#parseCommand(GroupMessageEvent, Context)} 方法，
 * 其他方法不会被代理</p>
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Inherited
public @interface Permission {
    int value();
}
