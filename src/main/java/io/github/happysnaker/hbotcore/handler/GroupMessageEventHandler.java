package io.github.happysnaker.hbotcore.handler;


import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.ContactList;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.contact.MemberPermission;
import net.mamoe.mirai.contact.NormalMember;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.AtAll;
import net.mamoe.mirai.message.data.MessageSource;
import net.mamoe.mirai.message.data.SingleMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 群消息处理器抽象类
 */
public abstract class GroupMessageEventHandler extends HBotUtil implements MessageEventHandler {

    /**
     * 获取事件的群号
     *
     * @param event 消息事件
     * @return 群ID
     */
    protected String getGroupId(GroupMessageEvent event) {
        return String.valueOf(event.getGroup().getId());
    }


    /**
     * 检查某人在此次消息事件中是否被 @
     *
     * @param event 事件
     * @param qq    需要检测的对象的 qq 号
     * @return 真则被 at
     */
    protected boolean isAt(GroupMessageEvent event, String qq) {
        for (SingleMessage message : event.getMessage()) {
            if (message instanceof At at) {
                if (Long.parseLong(qq) == at.getTarget()) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * 判断机器人是否被 at
     *
     * @return 真则被 at，请注意 at all 不会被此方法检测
     */
    protected boolean isAtBot(GroupMessageEvent event) {
        for (Bot bot : Bot.getInstances()) {
            if (isAt(event, String.valueOf(bot.getId()))) {
                return true;
            }
        }
        return false;
    }


    /**
     * 获取群成员列表，其中不包含机器人自己
     *
     * @param event 群消息事件
     * @return 返回群成员列表，不包含机器人
     */
    protected ContactList<NormalMember> getMembers(GroupMessageEvent event) {
        if (event != null) {
            return event.getGroup().getMembers();
        }
        return new ContactList<>();
    }

    /**
     * 获取所有群成员的昵称，如果成员未设置群昵称，则使用该成员的个人名片中的昵称替代
     *
     * @param event 群消息事件
     * @return 所有群成员的昵称，不包括机器人
     */
    protected List<String> getMembersGroupName(GroupMessageEvent event) {
        ContactList<NormalMember> members = null;
        List<String> ans = new ArrayList<>();
        if ((members = getMembers(event)) != null) {
            for (NormalMember member : members) {
                String s = member.getNameCard();
                if (s.isEmpty()) {
                    s = member.queryProfile().getNickname();
                }
                ans.add(s);
            }
        }
        return ans;
    }

    /**
     * 获取所有群成员的 QQ，不包含机器人自己
     *
     * @param event 群消息事件
     * @return 所有群成员的 QQ 号集合，不包括机器人自身
     */
    protected List<Long> getMembersQQ(GroupMessageEvent event) {
        ContactList<NormalMember> members = null;
        List<Long> ans = new ArrayList<>();
        if ((members = getMembers(event)) != null) {
            for (NormalMember member : members) {
                member.getNameCard();
                ans.add(member.getId());
            }
        }
        return ans;
    }

    /**
     * 根据群昵称获取成员 QQ 号
     *
     * @param event     群消息事件
     * @param groupName 群成员的群名片，如果成员未配置群昵称，则为群成员的昵称
     * @return 群成员的 QQ，未搜索到返回 -1
     */
    protected Long getMemberQQByName(GroupMessageEvent event, String groupName) {
        ContactList<NormalMember> members;
        long ans = -1;
        if ((members = getMembers(event)) != null) {
            for (NormalMember member : members) {
                String s = member.getNameCard();
                if (s.isEmpty())
                    s = member.queryProfile().getNickname();
                if (s.equals(groupName))
                    return member.getId();
            }
        }
        return ans;
    }


    /**
     * 提取事件中的纯文本消息
     *
     * @param event 消息事件
     * @return 返回处理后的消息，消息将去除首尾空格
     */
    public String getPlantContent(GroupMessageEvent event) {
        return getOnlyPlainContent(event).trim();
    }

    /**
     * 获取发送者权限
     *
     * @param event 消息事件
     * @return 0 是成员、1 是管理员，2 是群主
     */
    protected int getSenderPermission(GroupMessageEvent event) {
        MemberPermission permission = event.getPermission();
        return permission.getLevel();
    }


    /**
     * 撤回一条消息，如果撤回失败请考虑是否是权限不够
     *
     * @param source 消息源
     * @return 成功返回 true，否则返回 false
     */
    protected boolean cancelMessage(MessageSource source) {
        try {
            MessageSource.recall(source);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * 撤回一条消息，如果撤回失败请考虑是否是权限不够
     *
     * @param event 消息事件
     * @return 成功返回 true，否则返回 false
     * @throws InsufficientPermissionsException 如果权限不够则抛出此异常
     */
    protected boolean cancelMessage(GroupMessageEvent event) throws InsufficientPermissionsException {
        if (event.getPermission().getLevel() > 0) {
            throw new InsufficientPermissionsException("权限不足");
        }
        return cancelMessage(event.getSource());
    }

    /**
     * 检查此事件是否是群事件，并检查事件消息是否以关键词开头
     *
     * @param event
     * @param keywords
     * @return 如果都为真返回 true
     */
    public boolean startWithKeywords(GroupMessageEvent event, Collection<String> keywords) {
        String content = getPlantContent(event);
        if (content != null) {
            for (String keyword : keywords) {
                if (content.startsWith(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 禁言群成员，请保证自身有足够的权限
     *
     * @param qq             群成员 qq
     * @param group          群
     * @param durationSecond 秒
     */
    protected void mute(String qq, Group group, int durationSecond) {
        for (NormalMember member : group.getMembers()) {
            if (member.getId() == Long.parseLong(qq)) {
                member.mute(durationSecond);
                return;
            }
        }
    }

    /**
     * 禁言发言者
     *
     * @param event          消息事件
     * @param durationSecond 时长、秒
     */
    protected void muteSender(GroupMessageEvent event, int durationSecond) {
        event.getSender().mute(durationSecond);
    }

    /**
     * 构造一个 at，at 发言人
     * @param event 事件
     * @return at
     */
    protected At atSender(GroupMessageEvent event) {
        return new At(getSenderIdAsLong(event));
    }

    /**
     *
     * @return atAll
     */
    protected AtAll alAll() {
        return AtAll.INSTANCE;
    }
}
