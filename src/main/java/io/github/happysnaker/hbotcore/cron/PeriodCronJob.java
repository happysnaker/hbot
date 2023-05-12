package io.github.happysnaker.hbotcore.cron;


import io.github.happysnaker.hbotcore.logger.Logger;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.message.data.MessageChain;
import org.quartz.*;

import java.util.*;

/**
 * HRobot 定期发送任务机制的实现类
 */
public class PeriodCronJob implements Job {
    public static final String KEY = "KEY_PeriodCronJobData";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobKey key = context.getJobDetail().getKey();
        PeriodCronJobData jobData = (PeriodCronJobData) context.getJobDetail().getJobDataMap().get(KEY);
        if (jobData.count.get() <= 0) {
            Logger.info(String.format("用户定时调度任务 %s 执行次数已达阈值，取消任务", key.getName()));

            try {
                context.getScheduler().deleteJob(key);
            } catch (SchedulerException e) {
                throw new JobExecutionException(e);
            }
            return;
        }

        try {
            List<MessageChain> messages = new ArrayList<>();
            if (jobData.rawMessages != null && !jobData.rawMessages.isEmpty()) {
                String rawMessage = jobData.rawMessages.get((int) (jobData.rawMessages.size() * Math.random()));
                messages.add(HBotUtil.parseMiraiCode(rawMessage));
            }

            if (messages.isEmpty()) {
                Logger.debug("没有任何消息需要发送，忽略本次任务 " + key.getName() + " ，请检查是否配置了空消息或者无法解析的语义");
                return;
            }

            int sendNum = jobData.sendNum;
            if (sendNum == messages.size()) {
                for (MessageChain message : messages) {
                    for (Contact contact : jobData.contacts) {
                        contact.sendMessage(message);
                    }
                }
            } else {
                messages.sort((a, b) -> Math.random() < 0.5 ? -1 : Math.random() < 0.5 ? 0 : 1);
                for (int i = 0; i < sendNum; i++) {
                    for (Contact contact : jobData.contacts) {
                        contact.sendMessage(messages.get(i));
                    }
                }
            }
            Logger.debug(String.format("用户定时调度任务 %s 执行完毕，剩余次数：%d", key.getName(), jobData.count.decrementAndGet()));
        } catch (Exception e) {
            Logger.error(e);
        }
    }
}
