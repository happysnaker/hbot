package com.github.happysnaker.hbotcore.proxy;

import com.github.happysnaker.hbotcore.boot.HBot;
import com.github.happysnaker.hbotcore.handler.Interest;
import com.github.happysnaker.hbotcore.handler.MessageEventHandler;
import com.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 持续对话，可同步或者异步性地等待下一个条件发生，从而实现长对话的功能
 *
 * @Author happysnaker
 * @Date 2023/4/23
 * @Email happysnaker@foxmail.com
 */
public class ContinuousDialogue {

    private static final Map<Interest, CompletableFuture<GroupMessageEvent>> continuousDialogue = new ConcurrentHashMap<>();


    public static boolean checkContinuousDialogue(GroupMessageEvent event) {
        for (Map.Entry<Interest, CompletableFuture<GroupMessageEvent>> it : continuousDialogue.entrySet()) {
            if (it.getKey().isInterest(event)) {
                it.getValue().complete(event);
                continuousDialogue.remove(it.getKey());
                return true;
            }
        }
        return false;
    }


    /**
     * 异步等待下一个条件发生
     * @param event 事件，在一个长对话上判定同一个事件的标准为：群相同、发送人相同
     * @param interest 条件
     * @return 异步事件
     */
    public static CompletableFuture<GroupMessageEvent> waitForNextAsync(GroupMessageEvent event, Interest interest) {
        Interest.InterestBuilder builder = Interest.builder().matchAll(true);

        if (event != null) {
            builder.onCondition(Interest.MODE.SENDER, HBotUtil.getSenderId(event))
                    .onCondition(Interest.MODE.GROUP, String.valueOf(event.getGroup().getId()));
        }
        if (interest != null) {
            builder.onCondition(interest);
        }
        Interest i = builder.builder();
        if (continuousDialogue.containsKey(i)) {
            return continuousDialogue.get(i);
        }
        CompletableFuture<GroupMessageEvent> f = new CompletableFuture<>();
        continuousDialogue.put(i, f);
        return f;
    }


    public static CompletableFuture<GroupMessageEvent> waitForNextAsync(GroupMessageEvent event) {
        return waitForNextAsync(event, null);
    }


    public static CompletableFuture<GroupMessageEvent> waitForNextAsync(Interest interest) {
        return waitForNextAsync(null, interest);
    }


    /**
     * 堵塞式的等待某个条件发生
     */
    public static GroupMessageEvent waitForNext(GroupMessageEvent event, Interest interest, long timeoutMills) throws TimeoutException {
        CompletableFuture<GroupMessageEvent> future = waitForNextAsync(event, interest);
        if (timeoutMills > 0) {
            try {
                return future.get(timeoutMills, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static GroupMessageEvent waitForNext(GroupMessageEvent event, Interest interest) throws Exception {
        return waitForNext(event, interest, -1);
    }

    public static GroupMessageEvent waitForNext(GroupMessageEvent event) throws Exception {
        return waitForNext(event, null, -1);
    }

    public static GroupMessageEvent waitForNext(Interest interest) throws Exception {
        return waitForNext(null, interest, -1);
    }
}
