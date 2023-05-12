package io.github.happysnaker.hbotcore.handler;


import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * 消息事件处理者，这是 HRobot 的核心处理者，HRobot 设计核心就是为了处理群聊消息并回复消息，
 * HRobot 会自动处理 MessageEventHandler，并保证处理者唯一，而其他事件（非群联事件）都需要手动监听事件并注册消费逻辑
 * @author Happysnaker
 * @email happysnaker@foxmail.com
 */
public interface MessageEventHandler {
    /**
     * 处理一个新的消息事件，返回要回复的消息，可以回复多条消息，也可以为 null 或空链表（代表不回复）
     * <p>请注意，如果回复多条消息，会有小概率导致这些消息乱序发送</p>
     * @param event 群聊事件
     * @param ctx 上下文
     * @return 返回需要回复的消息，可以返回多条消息
     */
    List<MessageChain> handleMessageEvent(GroupMessageEvent event, Context ctx);


    /**
     * 是否应该处理事件，子类应该扩展它      <p>
     * 如果一个消息处理者对消息感兴趣，则消息将不会传到下一个处理者中，则并不是绝对的，可以在 <code>handleMessageEvent</code> 方法中调用 {@link Context#continueExecute()} 让消息继续传递下去
     * @param event 群聊事件
     * @param ctx 上下文
     * @return <strong>如果需要处理，则返回 true；如果不需要处理，则返回 false</strong>
     */
    boolean shouldHandle(GroupMessageEvent event, Context ctx);
}
