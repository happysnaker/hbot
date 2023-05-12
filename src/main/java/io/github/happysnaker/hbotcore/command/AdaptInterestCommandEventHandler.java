package io.github.happysnaker.hbotcore.command;

import io.github.happysnaker.hbotcore.exception.CanNotParseCommandException;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.handler.Interest;
import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/4/9
 * @Email happysnaker@foxmail.com
 */
@SuppressWarnings("unchecked")
public abstract class AdaptInterestCommandEventHandler extends DefaultCommandEventHandlerManager {
    protected Interest interest;

    @Override
    public List<MessageChain> parseCommand(GroupMessageEvent event, Context context) throws CanNotParseCommandException, InsufficientPermissionsException {
        try {
            return (List<MessageChain>) interest.dispatch(event, this, context);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            if (e.getCause() != null && e.getCause() instanceof InsufficientPermissionsException ee) {
                throw ee;
            }
            if (e.getCause() != null && e.getCause() instanceof CanNotParseCommandException ee) {
                throw ee;
            }
            throw new CanNotParseCommandException(e.getCause() == null ? e : e.getCause());
        }
    }

    @Override
    public boolean shouldHandle(GroupMessageEvent event, Context ctx) {
        return interest.isInterest(event);
    }

    public Interest getInterest() {
        return interest;
    }

    public void setInterest(Interest interest) {
        this.interest = interest;
    }
}
