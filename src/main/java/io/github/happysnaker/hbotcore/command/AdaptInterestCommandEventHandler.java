package io.github.happysnaker.hbotcore.command;

import io.github.happysnaker.hbotcore.exception.CanNotParseCommandException;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.exception.NoDispatchActionException;
import io.github.happysnaker.hbotcore.handler.Interest;
import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import javax.naming.CannotProceedException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/4/9
 * @Email happysnaker@foxmail.com
 */
@SuppressWarnings("unchecked")
public abstract class AdaptInterestCommandEventHandler extends HBotCommandEventHandlerManager {
    protected Interest interest;

    @Override
    public List<MessageChain> parseCommand(GroupMessageEvent event, Context context) throws CanNotParseCommandException, InsufficientPermissionsException {
        try {
            Object dispatch = interest.action(event, this, context);
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
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            if (e.getCause() != null && e.getCause() instanceof InsufficientPermissionsException ee) {
                throw ee;
            }
            if (e.getCause() != null && e.getCause() instanceof CanNotParseCommandException ee) {
                throw ee;
            }
            throw new CanNotParseCommandException(e.getCause() == null ? e : e.getCause());
        } catch (NoDispatchActionException | CannotProceedException e) {
            throw new RuntimeException(e);
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
