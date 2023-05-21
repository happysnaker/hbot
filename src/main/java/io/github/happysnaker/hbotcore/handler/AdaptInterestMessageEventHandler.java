package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.StringUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/4/9
 * @Email happysnaker@foxmail.com
 */
@SuppressWarnings("unchecked")
public class AdaptInterestMessageEventHandler extends GroupMessageEventHandler {
    protected Interest interest;

    public Interest getInterest() {
        return interest;
    }

    public void setInterest(Interest interest) {
        this.interest = interest;
    }

    @Override
    public List<MessageChain> handleMessageEvent(GroupMessageEvent event, Context ctx) {
        try {
            Object dispatch = interest.action(event, this, ctx);
            if (dispatch == null) {
                return null;
            }
            if (dispatch instanceof List list) {
                if (dispatch instanceof MessageChain chain) {
                    return Collections.singletonList(chain);
                } else if (!list.isEmpty() && list.get(0) instanceof MessageChain) {
                    return list;
                }
            }
            return buildMessageChainAsSingletonList(dispatch);
        } catch (Exception e) {
            Exception exception = e;
            if (e instanceof InvocationTargetException
                    && exception.getCause() != null && exception.getCause() instanceof Exception) {
                exception = (Exception) exception.getCause();
            }
            Logger.error("%s %s", Logger.formatLog(event), StringUtil.getErrorInfoFromException(exception));
            return buildMessageChainAsSingletonList("发生了一些意料之外的错误，错误消息: " + exception.getMessage());
        }
    }

    @Override
    public boolean shouldHandle(GroupMessageEvent event, Context ctx) {
        return interest.isInterest(event);
    }
}
