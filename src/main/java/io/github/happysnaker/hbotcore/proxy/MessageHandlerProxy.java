package io.github.happysnaker.hbotcore.proxy;


import cn.hutool.core.util.StrUtil;
import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.command.DefaultCommandEventHandlerManager;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.handler.handler;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.intercept.Intercept;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import lombok.Getter;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import org.springframework.stereotype.Component;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 消息事件代理，此类与 {@link Context} 一同定义了整个 HRobot 中处理消息的逻辑，<strong>此类是所有消息的第一个处理器</strong>，将调用用户的处理器与拦截器执行相关逻辑
 *
 * @see Context
 */
@Component("proxyHandler")
public class MessageHandlerProxy implements MessageEventHandler {
    @Getter
    private final List<MessageEventHandler> handlers;
    @Getter
    private final List<Interceptor> preInterceptors;
    @Getter
    private final List<Interceptor> postInterceptors;



    /**
     * 获取处理器与拦截器
     */
    public MessageHandlerProxy() {
        int messageHandlerCount = 0;
        handlers = new ArrayList<>();
        for (Object bean : HBot.applicationContext.getBeansWithAnnotation(handler.class).values()) {
            if (bean instanceof MessageEventHandler handler) {
                handlers.add(handler);
                handler annotation = bean.getClass().getAnnotation(handler.class);
                messageHandlerCount += annotation.isCommandHandler() ? 0 : 1;
            } else {
                Logger.error("处理器 %s 未继承 MessageEventHandler 接口", bean.getClass().getName());
            }
        }
        handlers.sort((a, b) -> {
            handler annotation0 = a.getClass().getAnnotation(handler.class);
            handler annotation1 = b.getClass().getAnnotation(handler.class);
            if (annotation0.isCommandHandler() || annotation1.isCommandHandler()) {
                if (!annotation0.isCommandHandler() || !annotation1.isCommandHandler()) {
                    return annotation0.isCommandHandler() ? -1 : 1;
                }
            }
            int aa = annotation0.priority();
            int bb = annotation1.priority();
            return bb - aa;
        });
        List<Interceptor> interceptors = new ArrayList<>();
        for (Object bean : HBot.applicationContext.getBeansWithAnnotation(Intercept.class).values()) {
            if (bean instanceof Interceptor interceptor) {
                interceptors.add(interceptor);
            } else {
                Logger.error("拦截器 %s 未继承 Interceptor 接口", bean.getClass().getName());
            }
        }
        preInterceptors = new ArrayList<>(interceptors);
        postInterceptors = new ArrayList<>(interceptors);
        preInterceptors.sort((a, b) -> {
            int aa = a.getClass().getAnnotation(Intercept.class).preOrder();
            int bb = b.getClass().getAnnotation(Intercept.class).preOrder();
            return bb - aa;
        });
        postInterceptors.sort((a, b) -> {
            int aa = a.getClass().getAnnotation(Intercept.class).postOrder();
            int bb = b.getClass().getAnnotation(Intercept.class).postOrder();
            return bb - aa;
        });

        Logger.info("消息代理初始化完成，检测到 %d 个用户消息处理器，%d 个命令监听器，%d 个用户消息拦截器",
                messageHandlerCount, handlers.size() - messageHandlerCount, interceptors.size());
    }


    @Override
    public List<MessageChain> handleMessageEvent(GroupMessageEvent event, Context ctx) {
        boolean isCommand = false;
        if (HBotUtil.getOnlyPlainContent(event).startsWith(DefaultCommandEventHandlerManager.prefix)) {
            isCommand = true;
            MessageChain chain = event.getMessage();
            for (SingleMessage message : chain) {
                // remove command prefix
                if (message instanceof PlainText str) {
                    if (StrUtil.isEmpty(DefaultCommandEventHandlerManager.prefix)) {
                        break;
                    }
                    String plain = str.getContent().replaceFirst(DefaultCommandEventHandlerManager.prefix, "");
                    try {
                        Field content = PlainText.class.getDeclaredField("content");
                        content.setAccessible(true);
                        content.set(str, plain);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                }
            }
        }
        if (ContinuousDialogue.checkContinuousDialogue(event)) {
            return null;
        }
        if (ctx == null) {
            List<MessageEventHandler> commandHandlers = handlers.stream()
                    .filter(h -> h.getClass().getAnnotation(handler.class) != null && h.getClass().getAnnotation(handler.class).isCommandHandler())
                    .collect(Collectors.toList());
            List<MessageEventHandler> normalHandlers = handlers.stream()
                    .filter(h -> h.getClass().getAnnotation(handler.class) == null || !h.getClass().getAnnotation(handler.class).isCommandHandler())
                    .collect(Collectors.toList());
            ctx = new Context(isCommand ? commandHandlers : normalHandlers,
                    preInterceptors, postInterceptors);
        }
        int execute = ctx.execute(event);
        if (isCommand && execute == 0) {
            event.getSubject()
                    .sendMessage(HBotUtil.buildMessageChain(
                            HBotUtil.getQuoteReply(event),
                            "未能识别的命令格式 " + DefaultCommandEventHandlerManager.prefix + HBotUtil.getOnlyPlainContent(event)
                    ));
        }
        return null;
    }

    @Override
    public boolean shouldHandle(GroupMessageEvent event, Context ctx) {
        return true;
    }
}
