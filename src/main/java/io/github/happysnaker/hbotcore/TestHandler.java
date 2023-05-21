package io.github.happysnaker.hbotcore;

import io.github.happysnaker.hbotcore.config.ConfigManager;

import io.github.happysnaker.hbotcore.handler.*;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.permisson.Permission;
import io.github.happysnaker.hbotcore.permisson.PermissionManager;
import io.github.happysnaker.hbotcore.plugin.HBotPluginEntry;
import io.github.happysnaker.hbotcore.plugin.HBotPluginLoader;

import io.github.happysnaker.hbotcore.plugin.HBotPluginRegister;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.proxy.ContinuousDialogue;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import lombok.SneakyThrows;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MusicKind;
import net.mamoe.mirai.message.data.MusicShare;
import org.springframework.stereotype.Component;

import javax.naming.CannotProceedException;
import java.sql.Time;
import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/4/21
 * @Email happysnaker@foxmail.com
 */
//@InterestFilters(value = {
//        @InterestFilter(mode = Interest.MODE.PREFIX, condition = "你", callbackMethod = "m3"),
//        @InterestFilter(mode = Interest.MODE.SUFFIX, condition = "我", callbackMethod = "m1"),
//        @InterestFilter(mode = Interest.MODE.CONTAINS, condition = "一波", output = "一波还未平息，一波又来侵袭"),
//        @InterestFilter(mode = Interest.MODE.CONTAINS, condition = "图", output = "" +
//                "[hrobot::$quote](sender)[hrobot::$img](https://api.isoyu.com/bing_images.php)")
//
//},
//        matchAll = true,
//        matchAllOutput = "兄弟们，一波了"
//)
//@Permission(PermissionManager.BOT_GROUP_ADMINISTRATOR)
public class TestHandler extends AdaptInterestMessageEventHandler {
    @Override
    @SneakyThrows
    public List<MessageChain> handleMessageEvent(GroupMessageEvent event, Context ctx) {
        Interest x = Interest.builder()
                .onCondition(Interest.MODE.PREFIX, "早")
                .onCondition(Interest.MODE.SENDER, "123456")
                .matchAll(true)
                .builder();
        Interest y = Interest.builder()
                .onCondition(Interest.MODE.PREFIX, "晚安")
                .onCondition(Interest.MODE.SENDER, "123456")
                .matchAll(true)
                .builder();
        Interest interest = Interest.builder()
                .onCondition(x, "老婆早早早，每一天都爱你", false)
                .onCondition(y, "老婆晚安，爱你摸摸哒", false)
                .builder();
        return buildMessageChainAsSingletonList(interest.action(event, this));
    }

    @Override
    public boolean shouldHandle(GroupMessageEvent event, Context ctx) {
        Interest x = Interest.builder()
                .onCondition(Interest.MODE.PREFIX, "早")
                .onCondition(Interest.MODE.SENDER, "123456")
                .matchAll(true)
                .builder();
        Interest y = Interest.builder()
                .onCondition(Interest.MODE.PREFIX, "晚安")
                .onCondition(Interest.MODE.SENDER, "123456")
                .matchAll(true)
                .builder();
        Interest interest = Interest.builder()
                .onCondition(x, "老婆早早早，每一天都爱你", false)
                .onCondition(y, "老婆晚安，爱你摸摸哒", false)
                .builder();

        return interest.isInterest(event);
    }

    public List<MessageChain> method0(Interest.DispatchArgs args) {
        args.getMode();
        String condition = args.getCondition();
        return HBotUtil.buildMessageChainAsSingletonList("method0 被调用");
    }

    public List<MessageChain> method1(GroupMessageEvent event) {
        return HBotUtil.buildMessageChainAsSingletonList("method1 被调用");
    }

    public List<MessageChain> m0(Interest.DispatchArgs args) {
        return HBotUtil.buildMessageChainAsSingletonList("456789");
    }

    public List<MessageChain> m1(Interest.DispatchArgs args, MessageEvent event) throws Exception {


        Thread.sleep(3000L);
        HBotPluginEntry plugin = HBotPluginLoader.load("hrobot-v1.0");



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
                """;
        return HBotUtil.buildMessageChainAsSingletonList(
                "您太帅了，我的神！！！",
                HBotUtil.getQuoteReply(event),
                atSender(event),
                HBotUtil.parseMiraiCode(code, event));
    }
}

