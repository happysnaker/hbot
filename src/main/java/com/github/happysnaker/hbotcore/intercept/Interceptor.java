package com.github.happysnaker.hbotcore.intercept;

import com.github.happysnaker.hbotcore.proxy.Context;
import com.github.happysnaker.hbotcore.handler.MessageEventHandler;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * 群聊消息事件的拦截器，可以在消息到达 {@link MessageEventHandler} 之前或之后捕获它
 *
 * @author Happysnaker
 * @description
 * @date 2022/1/24
 * @email happysnaker@foxmail.com
 */
public interface Interceptor {
    /**
     * 在事件到达 handler 之前拦截事件，如果返回真则将该事件拦截
     *
     * @param event   群消息事件
     * @param context 上下文
     * @return 返回真拦截，返回假通过
     */
    default boolean interceptBefore(GroupMessageEvent event, Context context) {
        return false;
    }


    /**
     * 在 handler 返回消息之后拦截事件及消息，在这里可以对消息进行一些处理，或者选择返回 null 以过滤消息
     *
     * @param event   群消息事件
     * @param context 上下文
     * @param mc      由 handler 返回的回复消息
     * @return 返沪经过处理后的消息，或者返回 null 以过滤此回复
     */
    default List<MessageChain> interceptAfter(GroupMessageEvent event, List<MessageChain> mc, Context context) {
        return mc;
    }
}
