package io.github.happysnaker.hbotcore.cron;


import io.github.happysnaker.hbotcore.logger.Logger;
import lombok.SneakyThrows;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>HRobot 定时任务的入口，在 HRobot 中一个有两种调度任务，一种是基于 Cron 触发的用户任务，一种是机器人自身每隔 {@link #PERIOD_MINUTE}
 * 分钟便会运行的后台任务，我们分别定义为用户任务和后台任务</p>
 * <p>这两种调度任务适用与不同的环境，
 * 对于用户任务而言，通常允许用户配置一些定时或定期发送的消息；而对于后台任务而言，开发者可以添加一些任务供 HRobot 驱动，例如实时拉取配置，订阅任务，清理垃圾等</p>
 * <p>建议将所有的调度任务交由 HRobot 管理，这避免开发者自己进行一些复杂的操作，HRobot 基于 quartz 进行调度，并暴露了一些静态方法接口供开发者添加任务与取消任务，
 * 可以看看 {@link #addBackgroundTask(String,Runnable)} {@link #rmBackgroundTask(String)}
 * {@link #submitUserCronJob(ScheduleBuilder, JobDataMap, String)} {@link #interruptUserCronJob(String)}</p>
 * <p>可在配置文件中配置是否开启机器人后台任务，并修改间隔时间，默认为三分钟</p>
 * @author Happysnaker
 * @email happysnaker@foxmail.com
 */
@Component
public class HBotCronJob implements Job {
    /**
     * 后台线程执行的任务列表
     */
    private static final Map<String, Runnable> tasks = new ConcurrentHashMap<>();
    /**
     * 定时调度器
     */
    private static final org.quartz.Scheduler scheduler;
    /**
     * 后台任务执行的间隔
     */
    public static int PERIOD_MINUTE = 3;
    /**
     * 是否允许机器人允许后台任务，如果不允许 HRobot 则不会运行后台任务
     */
    public static boolean enable = true;


    static {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            // 涉及到时间调度，默认使用中国标准时间
            try {
                TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            } catch (Exception e) {
                e.printStackTrace();
            }
            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 启动 HRobot 中的后台任务，将会每隔 {@link #PERIOD_MINUTE} 执行一次
     * <p>
     *     <strong>HRobot 仅会启动一次任务，重复启动不会提交多个任务，HRobot 会自动根据配置文件决定是否启动后台任务，开发者无需手动执行</strong>
     * </p>
     * @throws Exception
     */
    public synchronized static void cronIfEnable() throws Exception {
        if (enable) {
            enable = false;
            JobDetail jobDetail = JobBuilder.newJob(HBotCronJob.class)
                    .build();


            Trigger trigger = TriggerBuilder.newTrigger()
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.repeatMinutelyForever(PERIOD_MINUTE))
                    .build();
            scheduler.scheduleJob(jobDetail, trigger);
        }
    }

    /**
     * 中断一个用户任务
     * @param key 任务标识
     * @return 如果成功删除返回 true
     *
     */
    @SneakyThrows
    public static boolean interruptUserCronJob(String key)  {
        return scheduler.deleteJob(JobKey.jobKey(key));
    }

    /**
     * 向 HRobot 提交一个用户任务，用户任务应该包括在什么时候向谁发送什么消息
     * @param scheduleBuilder 调度规则，定义了什么时候执行，查看 {@link ScheduleBuilder} 的实现类来提供参数，这些实现类大多提供了一些工厂方法，例如 <code>SimpleScheduleBuilder.repeatMinutelyForever(PERIOD_MINUTE)</code>
     * @param data            传递的数据，定义了到时间点了做什么事情，请调用获取 {@link PeriodCronJobData#getJobDataMap(int, List, List, int)}
     * @param key 任务标识。可以通过此标识取消任务，允许为空，为空则会默认生成一个随机 key 并返回
     * @throws SchedulerException 调度失败
     * @throws IllegalArgumentException 如果任务标识不为空且任务标识已经存在
     * @return 任务标识，如果不为空则返回原字符串，如果为空，则返回生成的随机字符串
     */
    public synchronized static String submitUserCronJob(ScheduleBuilder<? extends Trigger> scheduleBuilder, JobDataMap data, String key) throws SchedulerException {
        if (data == null) {
            data = new JobDataMap();
        }
        Set<JobKey> keys = scheduler.getJobKeys(GroupMatcher.anyGroup());
        for (JobKey jobKey : keys) {
            if (jobKey.getName().equals(key)) {
                throw new IllegalArgumentException();
            }
        }
        JobDetail jobDetail = JobBuilder.newJob(PeriodCronJob.class)
                .usingJobData(data)
                .withIdentity(key)
                .build();


        Trigger trigger = TriggerBuilder.newTrigger()
                .startNow()
                .withSchedule(scheduleBuilder)
                .withIdentity(key)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        Logger.info(String.format("已提交一条用户任务 %s，下一次执行时间 %s", jobDetail.getKey().toString(), trigger.getNextFireTime().toString()));
        return jobDetail.getKey().getName();
    }


    


    /**
     * 添加机器人定时后台任务
     * @param key  任务标识，如果为 null 将随机生成，可根据此标识移除任务
     * @param task 添加的任务
     * @return 返回任务标识
     */
    public synchronized static String addBackgroundTask(String key, Runnable task) {
        if (key == null) {
            do {
                key = UUID.randomUUID().toString();
            } while (tasks.containsKey(key));
        }
        tasks.put(key, task);
        return key;
    }


    /**
     * 移除一个后台任务
     * @param key 任务标识
     */
    public static void rmBackgroundTask(String key) {
        tasks.remove(key);
    }

    /**
     * 是否存在某个任务
     * @param key 任务标识
     */
    public static boolean hasBackgroundTask(String key) {
        return tasks.containsKey(key);
    }


    /**
     * 机器人定时后台任务
     */
    @Override
    public void execute(JobExecutionContext context) {
        try {
            Logger.debug("HRobot 后台任务正在执行...");
            for (Runnable task : tasks.values()) {
                task.run();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
