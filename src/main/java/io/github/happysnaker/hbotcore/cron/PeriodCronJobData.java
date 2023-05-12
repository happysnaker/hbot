package io.github.happysnaker.hbotcore.cron;

import io.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import org.quartz.JobDataMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 此类定义了当到达用户指定时间时，应该做一些什么事情，此类定义了应该要发送给谁，以及发送什么消息
 * <p><strong>请使用 {@link #getJobDataMap(int, List, List, int)} 获取此类实例</strong></p>
 *
 * @Author happysnaker
 * @Date 2023/2/28
 * @Email happysnaker@foxmail.com
 */
public class PeriodCronJobData {
    public volatile AtomicInteger count;
    public volatile List<String> rawMessages;
    public volatile List<Contact> contacts;
    public volatile int sendNum;


    private PeriodCronJobData(int count, List<String> messages, List<Contact> contacts, int sendNum) {
        this.count = new AtomicInteger(count);
        this.rawMessages = messages;
        this.contacts = contacts;
        this.sendNum = sendNum;
    }


    /**
     * 创建一个 JobDataMap 实例
     *
     * @param count    定时消息执行的次数
     * @param messages 待发送的消息，允许有多条
     * @param contacts 发送的对象，允许群发消息
     * @param sendNum  待发送消息的条目，此参数必须小于等于 messages 的长度，如果小于，则会从中随机选择一些发送，禁止大于 messages 的长度或为 0
     *                 ，如果长度一致，则会<strong>顺序<strong/>发送全部消息
     * @return 创建的实例
     */
    public static JobDataMap getJobDataMap(int count, List<String> messages, List<Contact> contacts, int sendNum) {
        if (sendNum <= 0 || sendNum > messages.size()) {
            throw new IllegalArgumentException(String.format("send num is %s, but the messages length is %d, send num error", sendNum, messages.size()));
        }
        if (contacts == null || contacts.isEmpty()) {
            throw new IllegalArgumentException("没有定义发送对象，配置用户任务失败，不合法的参数");
        }
        PeriodCronJobData jobData = new PeriodCronJobData(count, messages, contacts, sendNum);
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(PeriodCronJob.KEY, jobData);
        return jobDataMap;
    }


    /**
     * 此方法与上述方法含义相同，区别在于 contacts 参数不同，此方法支持 contacts 参数为群号
     * @see #getJobDataMap(int, List, List, int)
     */
    public static JobDataMap getJobDataMap(int count, List<String> messages, int sendNum, List<String> contacts) {
        Set<String> set = HBotUtil.getBotsAllGroupId();
        List<Contact> cs = new ArrayList<>();
        for (String contact : contacts) {
            if (!set.contains(contact)) {
                throw new IllegalArgumentException(String.format("未查询到群号为 %s 的对象，无法创建用户定时任务数据", contact));
            }
            for (Bot bot : Bot.getInstances()) {
                Group group = bot.getGroup(Long.parseLong(contact));
                if (group != null) {
                    cs.add(group);
                    break;
                }
            }
        }
        return getJobDataMap(count, messages, cs, sendNum);
    }
}
