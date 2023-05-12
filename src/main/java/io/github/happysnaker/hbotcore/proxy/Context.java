package io.github.happysnaker.hbotcore.proxy;


import io.github.happysnaker.hbotcore.exception.CanNotSendMessageException;
import io.github.happysnaker.hbotcore.handler.MessageEventHandler;
import io.github.happysnaker.hbotcore.intercept.Interceptor;
import io.github.happysnaker.hbotcore.logger.Logger;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.data.MessageChain;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在群消息事件生命周期之间传递的上下文
 * <p>此上下文定义了事件的生命周期</p>
 *
 * @author Happysnaker
 * @date 2022/7/2
 * @email happysnaker@foxmail.com
 */
public class Context {
    private final Map<String, Object> params = new ConcurrentHashMap<>();
    private final List<MessageEventHandler> handlerList;
    private final List<Interceptor> preInterceptorList;
    private final List<Interceptor> postInterceptorList;
    private String message;
    private int index;
    private boolean execute;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Context(List<MessageEventHandler> handlerList, List<Interceptor> preInterceptorList, List<Interceptor> postInterceptorList) {
        this.handlerList = handlerList;
        this.preInterceptorList = preInterceptorList;
        this.postInterceptorList = postInterceptorList;
    }

    /**
     * 设置参数，这将在 handler 之间传递
     */
    public Context set(String key, Object val) {
        params.put(key, val);
        return this;
    }

    /**
     * 获取参数
     */
    public Object get(String key) {
        return params.get(key);
    }

    /**
     * 在执行列表末尾添加一个 handler
     *
     * @return this
     */
    public Context addHandler(MessageEventHandler handler) {
        handlerList.add(handler);
        return this;
    }

    /**
     * 将参数 handler 添加至当前 handler 的下一个位置
     *
     * @return this
     */
    public Context addHandlerToNext(MessageEventHandler handler) {
        handlerList.add(index + 1, handler);
        return this;
    }

    /**
     * 移除一个处理器
     *
     * @return this
     */
    public Context removeHandler(MessageEventHandler handler) {
        handlerList.remove(handler);
        return this;
    }

    /**
     * <strong>此方法只允许在 {@link MessageEventHandler#handleMessageEvent(GroupMessageEvent, Context)} 方法中调用，
     * 这将使得事件继续传递到下一个处理器</strong>
     */
    public void continueExecute() {
        execute = true;
    }

    /**
     * 执行处理消息逻辑，一旦一个 handler 对此事件感兴趣（shouldHandle 返回 true），
     * 那么此事件就会交由该 handler 执行，execute 方法会立即将 hanlder 的回复信息发送出去，
     * <strong>此事件不会再交由其他 handler 执行，但这不是绝对的， handler 可以在
     * {@link MessageEventHandler#handleMessageEvent} 方法中显式的调用 {@link #continueExecute()}}
     * 方法以表明希望能够继续处理下一个 handler</strong>
     *
     * @param event 消息事件
     * @return 返回调用 handler 的总数
     */
    public int execute(GroupMessageEvent event) {
        this.execute = true;
        // already executed, disable execute it again.
        if (this.handlerList.isEmpty()) {
            return 0;
        }
        // intercept
        for (Interceptor filter : preInterceptorList) {
            if (filter.interceptBefore(event, this)) {
                return 0;
            }
        }
        int c = 0;
        List<MessageChain> res = null;
        while (index < handlerList.size() && execute) {
            MessageEventHandler handler = handlerList.get(index);
            if (handler.shouldHandle(event, this)) {
                execute = false;
                res = handler.handleMessageEvent(event, this);
                c++;
            }
            index++;
        }
        for (Interceptor interceptor : postInterceptorList) {
            res = interceptor.interceptAfter(event, res, this);
        }
        try {
            if (res != null && !res.isEmpty()) {
                reply(res, event);
            }
        } catch (CanNotSendMessageException e) {
            Logger.error(e);
        }
        return c;
    }


    /**
     * 具体的回复动作
     *
     * @throws CanNotSendMessageException
     */
    private void reply(List<MessageChain> replyMessages, MessageEvent event) throws CanNotSendMessageException {
        Contact contact = event.getSubject();
        if (replyMessages == null) {
            return;
        }
        try {
            for (MessageChain replyMessage : replyMessages) {
                if (replyMessage != null && !replyMessage.isEmpty()) {
                    contact.sendMessage(replyMessage);
                }
            }
        } catch (Exception e) {
            throw new CanNotSendMessageException("Can not send message " + replyMessages + ", the contact is " + contact + ", cause by " + e.getCause().toString());
        }
    }
}


