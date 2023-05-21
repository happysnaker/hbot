package io.github.happysnaker.hbotcore.command;


import io.github.happysnaker.hbotcore.exception.CanNotParseCommandException;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.handler.GroupMessageEventHandler;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import org.springframework.aop.framework.AopContext;
import org.springframework.aop.framework.AopProxyUtils;

import java.util.List;

/**
 * 命令处理类. HBot 中处理命令消息的核心逻辑
 * <p>此类不具备命令管理的逻辑，子类需要自行实现，默认的实现是没有任何动作</p>
 * <p>Hbot 提供了 {@link HBotCommandEventHandlerManager} 来实现默认的命令管理机制，如无额外需求，可继承此类来实现简单的命令管理机制</p>
 *
 * @author Happysnaker
 * @email happysnaker@foxmail.com
 * @see HBotCommandEventHandlerManager
 */
public abstract class AbstractCommandEventHandler extends GroupMessageEventHandler implements CommandHandler, CommandHandlerManager {
    /**
     * 模板方法模式，命令处理的入口
     *
     * @param event 经过 proxyContent 处理后的消息
     * @param ctx   上下文
     * @return 响应的消息
     */
    @Override
    public List<MessageChain> handleMessageEvent(GroupMessageEvent event, Context ctx) {
        CommandHandler handler = null;
        try {
            handler = (CommandHandler) AopContext.currentProxy();
        } catch (Exception ignore) {
            handler = this;
        }
        List<MessageChain> ans;
        try {
            ans = handler.parseCommand(event, ctx);
        } catch (CanNotParseCommandException e) {
            fail(event, e);
            return List.of(HBotUtil.buildMessageChain(HBotUtil.getQuoteReply(event), "命令解析出错，错误消息：" + e.getMessage()));
        } catch (InsufficientPermissionsException e) {
            fail(event, "权限不足，错误消息：" + e.getMessage());
            return List.of(HBotUtil.buildMessageChain(HBotUtil.getQuoteReply(event), "对不起，您没有足够的权限，错误消息：" + e.getMessage()));
        } catch (Exception e) {
            fail(event, e);
            return List.of(HBotUtil.buildMessageChain(HBotUtil.getQuoteReply(event), "异常错误，说明：" + e.getMessage()));
        }
        if (ans != null) {
            success(event);
        }
        return ans;
    }


    /**
     * 解析命令，子类必须要实现的方法
     *
     * @see CommandHandler#parseCommand
     */
    @Override
    public abstract List<MessageChain> parseCommand(GroupMessageEvent event, Context context) throws CanNotParseCommandException, InsufficientPermissionsException;


    @Override
    public void success(GroupMessageEvent event) {
        // do nothing
    }

    @Override
    public void fail(GroupMessageEvent event, String errorMsg) {
        // do nothing
    }
}
