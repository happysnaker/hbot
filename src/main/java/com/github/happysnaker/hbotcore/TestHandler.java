package com.github.happysnaker.hbotcore;

import com.github.happysnaker.hbotcore.config.ConfigManager;
import com.github.happysnaker.hbotcore.handler.*;
import com.github.happysnaker.hbotcore.permisson.Permission;
import com.github.happysnaker.hbotcore.permisson.PermissionManager;
import com.github.happysnaker.hbotcore.proxy.ContinuousDialogue;
import com.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import javax.naming.CannotProceedException;
import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
 @handler
@InterestFilters({
        @InterestFilter(mode = Interest.MODE.PREFIX, condition = "你", callbackMethod = "m3"),
        @InterestFilter(mode = Interest.MODE.SUFFIX, condition = "我", callbackMethod = "m1")}
)
public class TestHandler extends AdaptInterestMessageEventHandler {
    static Interest interest = Interest.builder()
            .onCondition(Interest.MODE.CONTAINS, "123", "m0")
            .onCondition(Interest.MODE.REGEX, "早.*", "m1")
            .onCondition(Interest.MODE.SENDER, "1127423954", "m2")
            .onCondition(Interest.builder()
                    .onCondition(Interest.MODE.SENDER, "1637318597")
                    .onCondition(Interest.MODE.REGEX, ".*帅.*")
                    .matchAll(true)
                    .builder(), "m3")
            .builder();

    public List<MessageChain> m0(Interest.DispatchArgs args) {
        return buildMessageChainAsSingletonList("456789");
    }

    @Permission(PermissionManager.BOT_SUPER_ADMINISTRATOR)
    public List<MessageChain> m1(Interest.DispatchArgs args, MessageEvent event) throws Exception {
        ConfigManager.loadConfig();
        String content = getContent(args.getEvent());
        sendMsgAsync(buildMessageChainAsSingletonList("早什么早，早上要说我爱你"), event.getSubject());
        ContinuousDialogue.waitForNext((GroupMessageEvent) event, Interest.builder()
                .onCondition(Interest.MODE.PREFIX, "对")
                .builder());

        return buildMessageChainAsSingletonList("对你个头！");
    }

    public List<MessageChain> m2(Interest.DispatchArgs args) {
        muteSender(args.getEvent(), 3);
        return buildMessageChainAsSingletonList(
                "傻逼不许发言!!!",
                getQuoteReply(args.getEvent()),
                atSender(args.getEvent()));
    }

    public List<MessageChain> m3(Interest.DispatchArgs args, GroupMessageEvent event) throws CannotProceedException {
        String code = """
                [hrobot::$text](https://v.api.aa1.cn/api/tiangou/)
                [hrobot::$map[data][text]](a.txt)
                """;
        return buildMessageChainAsSingletonList(
                "您太帅了，我的神！！！",
                getQuoteReply(event),
                atSender(event),
                parseMiraiCode(code, event));
    }
}