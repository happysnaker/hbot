package io.github.happysnaker.hbotcore;

import io.github.happysnaker.hbotcore.command.HBotCommandEventHandlerManager;
import io.github.happysnaker.hbotcore.exception.CanNotParseCommandException;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.handler.handler;
import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/5/20
 * @Email happysnaker@foxmail.com
 */
//@handler(priority = 1, isCommandHandler = true)
public class CommandHandler extends HBotCommandEventHandlerManager {
    @Override
    public List<MessageChain> parseCommand(GroupMessageEvent event, Context context) throws CanNotParseCommandException, InsufficientPermissionsException {
        return null;
    }

    @Override
    public boolean shouldHandle(GroupMessageEvent event, Context ctx) {
        return false;
    }
}
