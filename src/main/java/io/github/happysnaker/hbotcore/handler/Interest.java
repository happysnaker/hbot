package io.github.happysnaker.hbotcore.handler;

import io.github.happysnaker.hbotcore.boot.HBot;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.exception.NoDispatchActionException;
import io.github.happysnaker.hbotcore.permisson.Permission;
import io.github.happysnaker.hbotcore.permisson.PermissionManager;
import io.github.happysnaker.hbotcore.permisson.PermissionProxy;
import io.github.happysnaker.hbotcore.proxy.Context;
import io.github.happysnaker.hbotcore.utils.HBotUtil;
import io.github.happysnaker.hbotcore.utils.Pair;
import io.github.happysnaker.hbotcore.utils.StringUtil;
import lombok.*;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.event.events.MessageEvent;


import javax.naming.CannotProceedException;
import java.lang.reflect.*;
import java.util.*;

/**
 * 此类是用于处理 {@link MessageEventHandler#shouldHandle(GroupMessageEvent, Context)} 的便捷方式<p>
 * 通过构造一系列条件表明对何种消息感兴趣，<strong>并便捷的调用对应的方法处理此事件</strong>，但请注意，如果此对象 {@link #matchAll} 属性被开启，
 * 则函数将不被允许调用 {@link #action(GroupMessageEvent, Object, Object...)} 方法，因为需要匹配多个条件无法确定需要调用那个条件的回调函数，
 * 可以通过复杂的嵌套 Interest 来实现这个需求
 * </p><p>此类是基于检测事件的文本信息进行匹配，<strong>at 或者图片等非文本将会被忽略</strong></p>
 *
 * @Author happysnaker
 * @Date 2023/4/7
 * @Email happysnaker@foxmail.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Interest {
    // 包含某字符串
    private Map<String, Pair<String, Boolean>> matchAnyContains = new HashMap<>();
    // 完全匹配某字符串
    private Map<String, Pair<String, Boolean>> matchAnyEquals = new HashMap<>();
    // 前缀匹配某字符串
    private Map<String, Pair<String, Boolean>> matchAnyPrefix = new HashMap<>();
    // 后缀匹配某字符串
    private Map<String, Pair<String, Boolean>> matchAnySuffix = new HashMap<>();
    // 正则匹配某模式串
    private Map<String, Pair<String, Boolean>> matchAnyRegex = new HashMap<>();
    // 匹配发送人
    private Map<String, Pair<String, Boolean>> matchAnySender = new HashMap<>();
    // 匹配发生群组
    private Map<String, Pair<String, Boolean>> matchAnyGroup = new HashMap<>();
    // 复杂匹配，自由组合
    private Map<Interest, Pair<String, Boolean>> matchAnyInterest = new HashMap<>();


    /**
     * 是否要匹配所有条件，否则只要符合任意一个条件都会通过，真则需要满足所有条件
     */
    private boolean matchAll = false;

    /**
     * 如果要匹配所有条件，当所有条件匹配时的动作，可以是 callback 或者 output
     */
    private String matchAllAction;

    /**
     * 如果要匹配所有条件，当所有条件匹配时的动作，此字段为真则 matchAllAction 为 callback，否则为 output
     */
    private boolean matchAllCallback = true;

    public enum MODE {
        /**
         * 事件文本消息是否包含某内容
         */
        CONTAINS,

        /**
         * 事件文本消息是否等于某内容
         */
        EQUALS,

        /**
         * 事件文本消息是否以某内容为前缀
         */
        PREFIX,

        /**
         * 事件文本消息是否以某内容为后缀
         */
        SUFFIX,

        /**
         * 事件文本消息是否匹配某模式串
         */
        REGEX,

        /**
         * 事件是否由某 qq 号发送
         */
        SENDER,

        /**
         * 事件是否由某个群发送
         */
        GROUP
    }


    /**
     * 建造器
     */
    public static class InterestBuilder {
        private final Interest interest;

        public InterestBuilder() {
            this.interest = new Interest();
        }

        public Interest builder() {
            return interest;
        }


        /**
         * 添加一个条件，以及设置等待在此条件上的回调函数
         *
         * @param mode       兴趣模式
         * @param condition  条件
         * @param action     动作，可以是 callback，也可以是 output
         * @param isCallback 是否是回调函数，否则是 output 直接输出
         */
        @SuppressWarnings("unchecked")
        public InterestBuilder onCondition(MODE mode, String condition, String action, boolean isCallback) {
            for (Field field : interest.getClass().getDeclaredFields()) {
                if (field.getName().toUpperCase().contains(mode.name().toUpperCase())) {
                    field.setAccessible(true);
                    try {
                        Map map = (Map) field.get(interest);
                        map.put(condition, Pair.of(action, isCallback));
                        break;
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            return this;
        }


        /**
         * 添加一个条件，以及设置等待在此条件上的回调函数
         *
         * @param mode           兴趣模式
         * @param condition      条件
         * @param callBackMethod 具体的函数名
         */
        @SuppressWarnings("unchecked")
        public InterestBuilder onCondition(MODE mode, String condition, String callBackMethod) {
            return onCondition(mode, condition, callBackMethod, true);
        }


        /**
         * 添加一个条件
         *
         * @param mode      兴趣模式
         * @param condition 条件
         */
        public InterestBuilder onCondition(MODE mode, String condition) {
            return onCondition(mode, condition, null);
        }

        /**
         * 添加一个条件
         *
         * @param filter 条件
         */
        public InterestBuilder onCondition(InterestFilter filter) {
            if (!filter.callbackMethod().isEmpty()) {
                return onCondition(filter.mode(), filter.condition(), filter.callbackMethod());
            }
            if (!filter.output().isEmpty()) {
                return onCondition(filter.mode(), filter.condition(), filter.output(), false);
            }
            return onCondition(filter.mode(), filter.condition());
        }


        /**
         * 添加一个嵌套的条件
         *
         * @param condition  嵌套的条件
         * @param action     条件触发的动作，可以是回调也可以是 mirai code 输出
         * @param isCallback 是否是回调函数，还是 output 输出
         */
        public InterestBuilder onCondition(@NonNull Interest condition, String action, boolean isCallback) {
            this.interest.matchAnyInterest.put(condition, Pair.of(action, isCallback));
            return this;
        }

        /**
         * 添加一个嵌套的条件
         *
         * @param condition      嵌套的条件
         * @param callBackMethod 回调函数名
         */
        public InterestBuilder onCondition(@NonNull Interest condition, String callBackMethod) {
            return onCondition(condition, callBackMethod, true);
        }

        /**
         * 添加一个嵌套的条件
         *
         * @param condition 嵌套的条件
         */
        public InterestBuilder onCondition(@NonNull Interest condition) {
            return onCondition(condition, null);
        }

        /**
         * 是否要匹配所有条件，真则只有当所有条件都满足时，{@link #isInterest(GroupMessageEvent)} 才会返回 true，否则只要有任意条件匹配则通过
         *
         * @param matchAll 默认为 false
         */
        public InterestBuilder matchAll(boolean matchAll) {
            this.interest.matchAll = matchAll;
            return this;
        }


        /**
         * 是否要匹配所有条件，真则只有当所有条件都满足时，{@link #isInterest(GroupMessageEvent)} 才会返回 true，否则只要有任意条件匹配则通过
         *
         * @param matchAll   默认为 false
         * @param action     所有条件都满足时的动作
         * @param isCallback 该动作是回调函数，还是直接 output 输出
         */
        public InterestBuilder matchAll(boolean matchAll, String action, boolean isCallback) {
            this.interest.matchAll = matchAll;
            this.interest.matchAllAction = action;
            this.interest.matchAllCallback = isCallback;
            return this;
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DispatchArgs {
        private GroupMessageEvent event;
        private String condition;
        private Interest conditionInterest;
        private MODE mode;
        private String methodName;
        private Object handler;
    }


    /**
     * @return null if has no action but on condition.
     * or dispatch action if on condition;
     * null if not on condition;
     */
    @SuppressWarnings("unchecked")
    private Pair<Object, Pair<String, Boolean>> checkCondition(Object condition, GroupMessageEvent event, MODE mode) {
        String plain = HBotUtil.getOnlyPlainContent(event);
        String sender = HBotUtil.getSenderId(event);
        String gid = String.valueOf(event.getGroup().getId());
        if (condition == null || plain == null) {
            return null;
        }
        if (condition instanceof Map map) {
            if (map.isEmpty()) {
                throw new RuntimeException("Not has this condition");
            }
            if (map.keySet().iterator().next() instanceof Interest) {
                for (Object key : map.keySet()) {
                    Interest interest = (Interest) (key);
                    if (interest.isInterest(event)) {
                        return Pair.of(interest, map.get(interest) == null ? null : (Pair<String, Boolean>) map.get(interest));
                    }
                }
                return null;
            }
            Set<String> keySet = map.keySet();
            return switch (mode) {
                case CONTAINS -> {
                    for (String str : keySet)
                        if (plain.contains(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
                case EQUALS -> {
                    for (String str : keySet)
                        if (plain.equals(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
                case PREFIX -> {
                    for (String str : keySet)
                        if (plain.startsWith(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
                case SUFFIX -> {
                    for (String str : keySet)
                        if (plain.endsWith(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
                case REGEX -> {
                    for (String str : keySet)
                        if (plain.matches(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
                case SENDER -> {
                    for (String str : keySet)
                        if (sender.equals(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
                case GROUP -> {
                    for (String str : keySet)
                        if (gid.equals(str))
                            yield Pair.of(str, map.get(str) == null ? null : (Pair<String, Boolean>) map.get(str));
                    yield null;
                }
            };
        }
        return null;
    }


    /**
     * 是否对此事件的消息感兴趣
     *
     * @param event    事件
     * @param matchAll 是否要匹配所有的条件，真则只有当所有条件满足时返回 true，假则一票通过
     * @return 返回触发的条件与动作，null 代表不感兴趣，key 是条件:模式，val 是动作:是否回调，如果是 matchAll，则条件为 this
     */
    @SneakyThrows
    private Pair<Pair<Object, MODE>, Pair<String, Boolean>> checkInterestAction(GroupMessageEvent event, boolean matchAll) {
        Pair<Pair<Object, MODE>, Pair<String, Boolean>> result = null;
        for (Field field : getClass().getDeclaredFields()) {
            Object o = field.get(this);
            if (!(o instanceof Map) || ((Map<?, ?>) o).isEmpty()) {
                continue;
            }
            String name = field.getName();
            MODE conditionMode = null;
            Object condition = null;
            Pair<String, Boolean> action = null;
            for (MODE mode : MODE.values()) {
                if (name.toUpperCase().contains(mode.name().toUpperCase())) {
                    var pair = checkCondition(o, event, mode);
                    if (pair != null) {
                        conditionMode = mode;
                        condition = pair.getKey();
                        action = pair.getValue();
                    }
                    break;
                }
            }

            // 嵌套匹配
            if (field.getName().equalsIgnoreCase("matchAnyInterest")) {
                var pair = checkCondition(this.matchAnyInterest, event, null);
                if (pair != null) {
                    condition = pair.getKey();
                    action = pair.getValue();
                }
            }

            if (condition == null && matchAll) {
                return null;
            }

            if (condition != null && !matchAll) {
                if (action != null) {
                    return Pair.of(Pair.of(condition, conditionMode), action);
                }
                result = Pair.of(Pair.of(condition, conditionMode), null);
            }
        }
        if (!matchAll) {
            return result;
        } else {
            var action = StringUtil.isNullOrEmpty(matchAllAction) ? null : Pair.of(matchAllAction, matchAllCallback);
            return Pair.of(Pair.of(this, null), action);
        }
    }


    /**
     * 是否对此事件的消息感兴趣
     *
     * @param event    事件
     * @param matchAll 是否要匹配所有的条件，真则只有当所有条件满足时返回 true，假则一票通过
     * @return true or false
     */
    @SneakyThrows
    public boolean isInterest(GroupMessageEvent event, boolean matchAll) {
        return checkInterestAction(event, matchAll) != null;
    }

    /**
     * 检测是否满足任意一个条件
     *
     * @param event 事件
     * @return 真则满足
     */
    public boolean isInterest(GroupMessageEvent event) {
        return isInterest(event, this.matchAll);
    }


    /**
     * <p>用户可在某个条件上绑定一个动作，此方法会自动触发某个条件上的动作</p>
     * <p>动作分为两种，一种是调用回调函数；另一种是解析 code 直接返回 {@link net.mamoe.mirai.message.data.MessageChain}</p>
     * <p>如果存在多个条件 match，此方法的行为是不确定的，因此调用此方法请确保一个事件只会存在一个条件，此类在多数情况下能有效减少使用者的代码编写量，但某些情况下可能不适用</p>
     * <p>按照通俗的约定，回调函数的参数第一项应该是 {@link DispatchArgs}，如果有多个参数，则从第二项开始由 args 匹配</p>
     * <p>如果函数签名不一致，此方法会尝试自动注入，但无法保障重载方法的调用顺序以及参数顺序</p>
     *
     * @param event 事件
     * @param proxy 执行代理，回调函数的代理，通常是 handler 自身，如果配置的是 code 输出，则可填 null
     * @param args  用户自定义的回调函数列表参数，通常来说第一个参数是 {@link DispatchArgs}，args 应从第二项开始，如果参数不匹配，
     *              此方法会尝试自动注入
     * @return 返回结果
     * @throws NoDispatchActionException 没有配置对应的动作
     */
    public Object action(GroupMessageEvent event, Object proxy, Object... args) throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InsufficientPermissionsException, NoDispatchActionException, CannotProceedException {
        var check = checkInterestAction(event, this.matchAll);
        if (check == null) {
            throw new NoDispatchActionException("Not interested in it.");
        }
        Object condition = check.getKey().getKey();
        MODE currentMode = check.getKey().getValue();
        Pair<String, Boolean> action = check.getValue();
        if (action == null) {
            throw new NoDispatchActionException("No action here.");
        }
        // output, return message chain
        if (!action.getValue()) {
            return HBotUtil.parseMiraiCode(action.getKey(), event);
        }
        String methodName = action.getKey();
        DispatchArgs dispatchArgs = null;
        if (condition instanceof String) {
            dispatchArgs = new DispatchArgs(event, (String) condition, this, currentMode, methodName, proxy);
        } else {
            dispatchArgs = new DispatchArgs(event, null, (Interest) condition, currentMode, methodName, proxy);
        }

        if (args == null) {
            args = new Object[]{dispatchArgs};
        } else {
            var newArgs = new Object[args.length + 1];
            newArgs[0] = dispatchArgs;
            System.arraycopy(args, 0, newArgs, 1, args.length);
            args = newArgs;
        }

        Class[] clazz = Arrays.stream(args)
                .map(Object::getClass)
                .toArray(Class[]::new);
        Method declaredMethod = null;
        try {
            declaredMethod = proxy.getClass().getDeclaredMethod(methodName, clazz);
        } catch (NoSuchMethodException e) {
            Object[] oldArgs = args.clone();
            for (Method method : proxy.getClass().getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                Parameter[] parameters = method.getParameters();
                if (parameters.length == 0) {
                    args = new Object[0];
                    declaredMethod = method;
                    break;
                }
                args = new Object[parameters.length];
                for (int i = 0; i < parameters.length; i++) {
                    var parameter = parameters[i];
                    if (parameter.getType().equals(GroupMessageEvent.class)) {
                        args[i] = event;
                    } else if (parameter.getType().equals(MessageEvent.class)) {
                        args[i] = event;
                    } else if (parameter.getType().equals(DispatchArgs.class)) {
                        args[i] = dispatchArgs;
                    } else {
                        for (Object oldArg : oldArgs) {
                            if (oldArg.getClass().equals(parameter.getType())) {
                                args[i] = oldArg;
                                break;
                            }
                        }
                    }
                    if (args[i] == null) {
                        break;
                    }
                }
                if (args[parameters.length - 1] != null) {
                    declaredMethod = method;
                    break;
                }
            }
        }
        if (declaredMethod == null) {
            throw new NoSuchMethodException("Can not invoke method " + methodName);
        }
        declaredMethod.setAccessible(true);

        // assert permission
        Permission annotation = declaredMethod.getAnnotation(Permission.class);
        if (annotation != null) {
            PermissionManager bean = HBot.applicationContext.getBean(PermissionManager.class);
            Context ctx = PermissionProxy.getArgs(args).getValue();
            if (!bean.hasPermission(annotation.value(), event, ctx)) {
                throw new InsufficientPermissionsException("权限不足");
            }
        }
        return declaredMethod.invoke(proxy, args);
    }


    public static InterestBuilder builder() {
        return new InterestBuilder();
    }
}
