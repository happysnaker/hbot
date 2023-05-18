package io.github.happysnaker.hbotcore;

import io.github.happysnaker.hbotcore.config.ConfigManager;

import io.github.happysnaker.hbotcore.handler.*;
import io.github.happysnaker.hbotcore.permisson.Permission;
import io.github.happysnaker.hbotcore.permisson.PermissionManager;
import io.github.happysnaker.hbotcore.plugin.HBotPluginEntry;
import io.github.happysnaker.hbotcore.plugin.HBotPluginLoader;
import io.github.happysnaker.hbotcore.plugin.HBotPluginRegistry;
import io.github.happysnaker.hbotcore.proxy.ContinuousDialogue;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import javax.naming.CannotProceedException;
import java.sql.Time;
import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
@InterestFilters({
        @InterestFilter(mode = Interest.MODE.PREFIX, condition = "你", callbackMethod = "m3"),
        @InterestFilter(mode = Interest.MODE.SUFFIX, condition = "我", callbackMethod = "m1")}
)
public class TestHandler extends AdaptInterestMessageEventHandler {
    public List<MessageChain> m0(Interest.DispatchArgs args) {
        return HBotUtil.buildMessageChainAsSingletonList("456789");
    }

    public List<MessageChain> m1(Interest.DispatchArgs args, MessageEvent event) throws Exception {
        HBotPluginRegistry.unRegistry("hrobot");
        HBotPluginLoader.unLoad("hrobot");

        Thread.sleep(3000L);
        HBotPluginEntry plugin = HBotPluginLoader.load("hrobot-v1.0");
        HBotPluginRegistry.registry(plugin);


        HBotUtil.sendMsgAsync(HBotUtil.buildMessageChainAsSingletonList("早什么早，早上要说我爱你"), event.getSubject());
        ContinuousDialogue.waitForNext((GroupMessageEvent) event, Interest.builder()
                .onCondition(Interest.MODE.PREFIX, "对")
                .builder());
        return HBotUtil.buildMessageChainAsSingletonList("注册成功!");
    }

    public List<MessageChain> m2(Interest.DispatchArgs args) {
        muteSender(args.getEvent(), 3);
        return HBotUtil.buildMessageChainAsSingletonList(
                "傻逼不许发言!!!",
                HBotUtil.getQuoteReply(args.getEvent()),
                atSender(args.getEvent()));
    }

    public List<MessageChain> m3(Interest.DispatchArgs args, GroupMessageEvent event) throws CannotProceedException {
        String code = """
                [hrobot::$text](https://v.api.aa1.cn/api/tiangou/)
                [hrobot::$map[data][text]](a.txt)
                """;
        return HBotUtil.buildMessageChainAsSingletonList(
                "您太帅了，我的神！！！",
                HBotUtil.getQuoteReply(event),
                atSender(event),
                HBotUtil.parseMiraiCode(code, event));
    }
}