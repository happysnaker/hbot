package io.github.happysnaker.hbotcore.command;


import io.github.happysnaker.hbotcore.utils.StringUtil;
import net.mamoe.mirai.event.events.GroupMessageEvent;

/**
 * 命令处理器管理者 <p>
 *     HRobot 中，命令是通过向群聊中发送特定指令触发的，命令执行操作需要被管理起来，当命令执行成功或失败时都可能需要触发相应的逻辑，例如我们需要将命令记录下来，以便管理员查看何时执行了一条什么命令。
 * </p>
 * <p>HRobot 提供了一个默认的管理器，可以看看 {@link HBotCommandEventHandlerManager}，如果需要使用这个默认的处理器，你可以使你的处理器直接继承它，
 * 如果你不需要相关逻辑，则你的命令处理器可以直接继承 {@link AbstractCommandEventHandler}，如果你有自定义的实现，则可以继承 {@link AbstractCommandEventHandler}，覆盖其中的  success 和 fail 方法，或许你可以自定义一个抽象类来复用代码</p>
 * @author Happysnaker
 * @email happysnaker@foxmail.com
 */
public interface CommandHandlerManager {
    /**
     * 当命名事件执行成功时需要做的事
     * @param event 事件
     */
    void success(GroupMessageEvent event);

    /**
     * 当命名事件执行失败时需要做的事
     * @param event 事件
     */
    void fail(GroupMessageEvent event, String errorMsg);

    /**
     * 当命名事件执行失败时需要做的事
     * @param event 事件
     */
    default void fail(GroupMessageEvent event, Throwable e) {
        fail(event, StringUtil.getErrorInfoFromException(e));
    };
}
