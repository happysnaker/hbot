package io.github.happysnaker.hbotcore;


import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.handler.GroupMessageEventHandler;
import io.github.happysnaker.hbotcore.intercept.Intercept;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import lombok.SneakyThrows;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MessageSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Intercept(preOrder = 1, postOrder = 1)
public class DemoInterceptor extends GroupMessageEventHandler implements Interceptor {

    public static final Set<String> sensitiveWords = Set.of("jb", "sb");
    @Override
    @SneakyThrows
    public boolean interceptBefore(GroupMessageEvent event, Context context) {
        var content = HBotUtil.getOnlyPlainContent(event);
        for (var word : sensitiveWords) {
            if (content.contains(word)) {
                // 撤回该消息
                MessageSource.recall(event.getSource());
                HBotUtil.sendMsgAsync(HBotUtil.buildMessageChainAsSingletonList(
                        "您的发言违反了本群规定",
                        new At(event.getSender().getId())
                ), event.getSubject());
                return true; // 过滤
            }
        }
        return false;
    }

    @Override
    public List<MessageChain> interceptAfter(GroupMessageEvent event, List<MessageChain> mc, Context context) {
        // 给每一个响应消息加上引用回复
        List<MessageChain> newReply = new ArrayList<>();
        for (MessageChain messages : mc) {
            newReply.add(HBotUtil.buildMessageChain(messages, HBotUtil.getQuoteMessageChain(event)));
        }
        return Interceptor.super.interceptAfter(event, newReply, context);
    }
}
