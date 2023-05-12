package io.github.happysnaker.hbotcore.command;


import io.github.happysnaker.hbotcore.exception.CanNotParseCommandException;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;

/**
 * 机器人命令处理接口
 * @author Happysnaker
 * @description
 * @date 2022/2/23
 * @email happysnaker@foxmail.com
 */
public interface CommandHandler {
    /**
     * 解析命令
     * @param event 命令事件
     * @param context 上下文
     * @return 解析完成后返回的消息
     * @throws CanNotParseCommandException 无法解析时请抛出此异常
     * @throws InsufficientPermissionsException 没有足够权限时请抛出此异常
     */
    List<MessageChain> parseCommand(GroupMessageEvent event, Context context) throws CanNotParseCommandException, InsufficientPermissionsException;
}
