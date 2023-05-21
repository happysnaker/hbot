package io.github.happysnaker.hbotcore.gpt;

import io.github.happysnaker.hbotcore.handler.AdaptInterestMessageEventHandler;
import io.github.happysnaker.hbotcore.handler.Interest;
import io.github.happysnaker.hbotcore.handler.handler;
import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.proxy.Context;
import lombok.NonNull;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内置的 Chatgpt handler，使用逆向工程
 *
 * @Author happysnaker
 * @Date 2023/4/18
 * @Email happysnaker@foxmail.com
 */
@handler(priority = -1)
public class RevChatGptHandler extends AdaptInterestMessageEventHandler {
    protected static List<AuthConfig> authConfigs;

    protected static Map<Long, ChatBot> sessionIsolation = new ConcurrentHashMap<>();

    protected static Map<Long, Byte> groupLock = new ConcurrentHashMap<>();

    protected static Map<Long, Integer> groupConfigIdx = new ConcurrentHashMap<>();

    protected final static String presetContent = """
            现在，你的身份是一个聊天助手，我将作为你的聊天对象，由于聊天比较发散，我问的问题可能上下文不通，如果你实在理解不了上下文，没关系，
            请大胆地忘记前文，发挥你的想象力与创造力，与我一起聊天。当然对于客观事实请不要捏造，涉及到学术知识请精准的给予我答案。
            """;

    protected final static String resetConversation = """
            现在，请你忘记我们之前所有的谈话内容，让我们重新开始一段会话，请不要参考任何前文，请忘记它们，然后与我重新开始聊天。
            现在，你的身份依然是一个聊天助手，我将作为你的聊天对象，由于聊天比较发散，我问的问题可能上下文不通，如果你实在理解不了上下文，没关系，
            请大胆地忘记前文，发挥你的想象力与创造力，与我一起聊天。当然对于客观事实请不要捏造，涉及到学术知识请精准的给予我答案。
            """;


    public static boolean enable = false;

    /**
     * 刷新 Gpt 的登录配置
     */
    public static void updateAuthConfig(@NonNull List<AuthConfig> authConfigs) {
        RevChatGptHandler.authConfigs = authConfigs;
    }


    private synchronized static Object getLock(long groupId) {
        groupLock.putIfAbsent(groupId, (byte) 0x0b);
        return groupLock.get(groupId);
    }

    private static ChatBot getChatBot(long groupId) {
        if (sessionIsolation.get(groupId) != null) {
            return sessionIsolation.get(groupId);
        }
        int id = groupConfigIdx.getOrDefault(groupId, 0);
        if (authConfigs.isEmpty()) {
            throw new RuntimeException("GPT auth config is empty.");
        }
        AuthConfig auth = authConfigs.get(id);
        ChatBot bot = new ChatBot(auth);
        Logger.info("Get the " + id + " auth to gpt.");
        sessionIsolation.put(groupId, bot);
        groupConfigIdx.put(groupId, (id + 1) % authConfigs.size()); // for next
        bot.ask(presetContent);
        return bot;
    }


    public RevChatGptHandler() {
        super.interest = Interest.builder()
                .onCondition(Interest.MODE.EQUALS, "重置会话", "reset")
                .onCondition(Interest.MODE.EQUALS, "reset", "reset")
                .onCondition(Interest.MODE.REGEX, ".+", "ask")
                .builder();
    }

    public List<MessageChain> reset(Interest.DispatchArgs args) {
        long gid = args.getEvent().getGroup().getId();
        synchronized (getLock(gid)) {
            if (Math.random() <= 0.2) {
                sessionIsolation.remove(gid);
                Logger.info("Abandon the gpt session, we will renew chatgpt next session.");
                return buildMessageChainAsSingletonList("清除会话成功！");
            }
            if (!sessionIsolation.containsKey(gid)) {
                return buildMessageChainAsSingletonList("本群暂无对话信息");
            }
            sessionIsolation.get(gid).ask(resetConversation);
            return buildMessageChainAsSingletonList("清除会话成功！");
        }
    }

    public List<MessageChain> ask(Interest.DispatchArgs args) throws Exception {
        var event = args.getEvent();
        long gid = args.getEvent().getGroup().getId();
        int rty = authConfigs.size();
        Exception ex = null;
        synchronized (getLock(gid)) {
            while (rty-- != 0) {
                try {
                    ChatBot bot = getChatBot(gid);
                    String reply = bot.ask(getPlantContent(event));
                    return buildMessageChainAsSingletonList(getQuoteReply(event), reply);
                } catch (Exception e) {
                    ex = e;
                    // get next
                    sessionIsolation.remove(gid);
                }
            }
        }
        throw ex == null ? new RuntimeException() : ex;
    }

    @Override
    public boolean shouldHandle(GroupMessageEvent event, Context ctx) {
        return enable && isAtBot(event) && super.shouldHandle(event, ctx);
    }
}
