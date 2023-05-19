package io.github.happysnaker.hbotcore.utils;



import com.alibaba.fastjson.JSONObject;
import io.github.happysnaker.hbotcore.exception.CanNotSendMessageException;
import io.github.happysnaker.hbotcore.exception.FileUploadException;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.events.MessageEvent;
import net.mamoe.mirai.message.code.MiraiCode;
import net.mamoe.mirai.message.data.*;
import net.mamoe.mirai.utils.ExternalResource;

import javax.naming.CannotProceedException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Happysnaker
 * @description
 * @date 2022/6/30
 * @email happysnaker@foxmail.com
 */
public class HBotUtil {

    private static final String TAG_PATTERN = "(\\[hrobot::\\$.*?])(\\((.*?)\\))";

    /**
     * 读取机器人所有的群
     *
     * @return 群号集合
     */
    public static Set<String> getBotsAllGroupId() {
        List<Bot> bots = Bot.getInstances();
        Set<String> ans = new HashSet<>();
        for (Bot bot : bots) {
            for (Group group : bot.getGroups()) {
                ans.add(String.valueOf(group.getId()));
            }
        }
        return ans;
    }

    /**
     * 从事件中提取消息，并将该消息转换为 mirai 码
     *
     * @param event 消息事件
     * @return mirai 编码消息
     */
    public static String getContent(MessageEvent event) {
        if (event == null) {
            return null;
        }
        return getContent(event.getMessage());
    }

    /**
     * 该消息转换为 mirai 码
     *
     * @param chain 消息链
     * @return mirai 编码消息
     */
    public static String getContent(MessageChain chain) {
        if (chain == null) {
            return null;
        }
        return chain.serializeToMiraiCode();
    }

    /**
     * <P>某些时候需要自定义上传一些图片，上传一个图片需要一个 Contact 对象，可以调用此方法获取 Bot 自身 Contact</P>
     * <P>如果没有机器人登录，这个方法会返回 NULL</P>
     */
    public static Contact getAdaptContact() {
        for (Bot bot : Bot.getInstances()) {
            return bot.getFriend(bot.getId());
        }
        return null;
    }

    /**
     * 提取 HRobot 中的 tag 和 val
     *
     * @param matcher matcher
     * @return tag-val 键值对
     */
    private static Pair<String, String> findValidTagAndVal(Matcher matcher) {
        String tag = matcher.group(1);
        String val = matcher.group(2);
        tag = tag.substring(10, tag.length() - 1);
        val = val.substring(1, val.length() - 1);
        if (tag.isEmpty()) {
            return null;
        }
        return Pair.of(tag, val);
    }

    /**
     * 从 mirai 编码转换为 MessageChain，同时这个函数将会解析 HRobot 自带的标签
     *
     * @param content 编码内容
     * @param event   HRobot 自带标签中，可能会存在 quote、at 等动态元素，这需要知道具体的事件源，如果消息不带有与事件源相关的标签，则此参数可置空
     * @return MessageChain 解析出的消息链
     * @throws CannotProceedException 解析 HRobot 标签出错时抛出
     */
    public static MessageChain parseMiraiCode(String content, MessageEvent event) throws CannotProceedException {
        Pattern pattern = Pattern.compile(TAG_PATTERN);
        Matcher matcher = pattern.matcher(content);
        int fromIndex = 0;
        MessageChainBuilder builder = new MessageChainBuilder();
        while (matcher.find()) {
            Pair<String, String> pair = findValidTagAndVal(matcher);
            if (pair == null) continue;
            String tag = pair.getKey(), val = pair.getValue();
            builder.append(MiraiCode.deserializeMiraiCode(content.substring(fromIndex, matcher.start())));
            fromIndex = matcher.end();
            try {
                String lowerCase = tag.toLowerCase();
                if (lowerCase.startsWith("map")) {
                    List<String> ps = new ArrayList<>();
                    String key = lowerCase.substring("map".length());
                    while (key.contains("[")) {
                        int x = key.indexOf('[');
                        int y = key.indexOf(']');
                        ps.add(key.substring(x + 1, y).trim());
                        key = key.substring(y + 1);
                    }
                    MapGetter mg = null;
                    if (val.startsWith("http")) {
                        mg = IOUtil.sendAndGetResponseMapGetter(new URL(val), "GET", null, null);
                    } else {
                        mg = new MapGetter(JSONObject.parseObject(IOUtil.readFile(new File(val)), Map.class));
                    }
                    for (int i = 0; i < ps.size(); i++) {
                        if (i == ps.size() - 1) {
                            builder.append(mg.getString(ps.get(i), true));
                        } else {
                            mg = mg.getMapGetter(ps.get(i));
                        }
                    }
                    continue;
                }
                switch (lowerCase) {
                    case "img" -> {
                        if (val.startsWith("http")) {
                            builder.append(uploadImage(event, new URL(val)));
                        } else {
                            builder.append(uploadImage(event, val));
                        }
                    }
                    case "text" -> {
                        if (val.startsWith("http")) {
                            builder.append(IOUtil.sendAndGetResponseString(new URL(val), "GET", null, null));
                        } else {
                            builder.append(Files.readString(Path.of(val)));
                        }
                    }
                    case "quote" -> {
                        builder.append(getQuoteReply(event));
                    }
                    case "at" -> {
                        if (val.equals("sender")) {
                            builder.append(new At(getSenderIdAsLong(event)));
                        } else {
                            builder.append(Long.parseLong(val) == -1L ? AtAll.INSTANCE : new At(Long.parseLong(val)));
                        }
                    }
                    default ->
                            builder.append(MiraiCode.deserializeMiraiCode(content.substring(matcher.start(), matcher.end())));
                }
            } catch (Exception e) {
                throw new CannotProceedException(String.format("解析语义标签 %s 出错，异常原因 %s, 可能是网络超时或者值的格式不正确", matcher.group(0), e.getMessage()));
            }
        }
        builder.append(MiraiCode.deserializeMiraiCode(content.substring(fromIndex)));
        return builder.build();
    }

    /**
     * 从 mirai 编码转换为 MessageChain，同时这个函数将会解析 HRobot 自带的标签
     * <p>请注意，由于不存在消息源，此方法无法解析 at、quote 等动态语义</p>
     * <p>此方法能够自解析 img 标签，会默认调用 {@link #getAdaptContact()} 作为对象发送</p>
     *
     * @param content 编码内容
     * @return MessageChain 解析出的消息链
     * @throws CannotProceedException 解析 HRobot 标签出错时抛出
     */
    public static MessageChain parseMiraiCode(String content) throws CannotProceedException {
        Contact contact = null;
        Pattern pattern = Pattern.compile(TAG_PATTERN);
        Matcher matcher = pattern.matcher(content);
        int fromIndex = 0;
        MessageChainBuilder builder = new MessageChainBuilder();
        while (matcher.find()) {
            Pair<String, String> pair = findValidTagAndVal(matcher);
            if (pair == null) continue;
            String tag = pair.getKey(), val = pair.getValue();
            builder.append(MiraiCode.deserializeMiraiCode(content.substring(fromIndex, matcher.start())));
            fromIndex = matcher.end();
            try {
                switch (tag) {
                    case "img" -> {
                        if (contact == null) {
                            contact = getAdaptContact();
                        }
                        if (val.startsWith("http")) {
                            builder.append(uploadImage(contact, new URL(val)));
                        } else {
                            builder.append(uploadImage(contact, val));
                        }
                    }
                    case "at" -> {
                        if (val.equals("sender")) {
                            throw new CannotProceedException("没有消息源，无法引用发送人");
                        }
                        builder.append(Long.parseLong(val) == -1L ? AtAll.INSTANCE : new At(Long.parseLong(val)));
                    }
                    case "quote" -> throw new CannotProceedException("没有消息源，无法引用发送人");
                    default ->
                            builder.append(MiraiCode.deserializeMiraiCode(content.substring(matcher.start(), matcher.end())));
                }
            } catch (Exception e) {
                throw new CannotProceedException(String.format("解析语义标签 %s 出错，异常原因 %s, 可能是网络超时或者值的格式不正确", matcher.group(0), e.getMessage()));
            }
        }
        builder.append(MiraiCode.deserializeMiraiCode(content.substring(fromIndex)));
        return builder.build();
    }

    /**
     * 从事件中提取消息，该消息仅包含纯文本内容
     *
     * @param event 消息事件
     * @return 去除了表情、图片、at 等其他元素的纯文本消息
     * @see #getContent
     */
    public static String getOnlyPlainContent(MessageEvent event) {
        if (event == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (SingleMessage singleMessage : event.getMessage()) {
            if (singleMessage instanceof PlainText) {
                sb.append(singleMessage);
            }
        }
        return sb.toString().trim();
    }

    /**
     * 读取文件（图片）并上传至腾讯服务器
     *
     * @param event    对应事件
     * @param filename 图片文件路径名
     * @return net.mamoe.mirai.message.data.Image 已上传的图片，可直接发送
     */
    public static Image uploadImage(MessageEvent event, String filename) throws FileUploadException {
        try {
            return ExternalResource.uploadAsImage(new File(filename), event.getSubject());
        } catch (Exception e) {
            throw new FileUploadException("无法上传文件: " + filename + "，可能的原因是 " + e.getCause().toString());
        }
    }

    /**
     * 读取文件（图片）并上传至腾讯服务器
     *
     * @param contact  上传对象，上传对象可以是任意对象，仅上传并不会发送图片
     * @param filename 图片文件路径名
     * @return net.mamoe.mirai.message.data.Image
     */
    public static Image uploadImage(Contact contact, String filename) throws FileUploadException {
        try {
            return ExternalResource.uploadAsImage(new File(filename), contact);
        } catch (Exception e) {
            throw new FileUploadException("无法上传文件: " + filename + "，可能的原因是 " + e.getCause().toString());
        }
    }

    /**
     * 网络图片并上传至腾讯服务器
     *
     * @param event 对应事件
     * @param url   网络图片 URL
     * @return net.mamoe.mirai.message.data.Image
     */
    public static Image uploadImage(MessageEvent event, URL url) throws FileUploadException {
        return uploadImage(event.getSubject(), url);
    }


    /**
     * 网络图片并上传至腾讯服务器
     *
     * @param url   网络图片 URL
     * @return net.mamoe.mirai.message.data.Image
     */
    public static Image uploadImage(URL url) throws FileUploadException {
        return uploadImage(getAdaptContact(), url);
    }

    /**
     * 网络图片并上传至腾讯服务器
     *
     * @param contact 要发送的对象，仅会上传而不会实际发送
     * @param url     网络图片 URL
     * @return net.mamoe.mirai.message.data.Image
     */
    public static Image uploadImage(Contact contact, URL url) throws FileUploadException {
        try (InputStream stream = IOUtil.sendAndGetResponseStream(
                url,
                "GET",
                null,
                null
        )) {
            return Contact.uploadImage(contact, stream);
        } catch (IOException e) {
            throw new FileUploadException(e);
        }
    }


    /**
     * 将多个 Message 元素构造成一个 MessageChain<p>
     * 一个 MessageChain 由多个消息元素组成，例如可以是由图片、表情、文本组成的一个消息，则可以这样构造：<br>
     * <code>buildMessageChain(new Face(2), "图片为：", uploadImage(event, new URL("http....")))</code>
     *
     * @param m 多个 SingleMessage 或 MessageChain
     * @return 将多个 Message 组合成 MessageChain
     */
    public static MessageChain buildMessageChain(Object... m) {
        MessageChainBuilder builder = new MessageChainBuilder();
        for (Object s : m) {
            if (s == null) {
                continue;
            }
            if (s instanceof String) {
                builder.append(new PlainText((CharSequence) s));
            } else if (s instanceof StringBuilder) {
                builder.append(new PlainText((CharSequence) s.toString()));
            } else if (s instanceof SingleMessage) {
                builder.append((SingleMessage) s);
            } else if (s instanceof Message) {
                builder.append((Message) s);
            } else if (s instanceof String[] strs) {
                for (String str : strs) {
                    builder.append(str);
                }
            } else {
                builder.append(new PlainText(s.toString()));
            }
        }
        return builder.build();
    }

    /**
     * build as singleton list
     *
     * @param m
     * @return singleton list
     */
    public static List<MessageChain> buildMessageChainAsSingletonList(Object... m) {
        return Collections.singletonList(buildMessageChain(m));
    }

    /**
     * 获取发送者的 QQ
     *
     * @param event 消息事件
     * @return 发送者的 QQ
     */
    public static String getSenderId(MessageEvent event) {
        return String.valueOf(event.getSender().getId());
    }

    /**
     * 获取发送者的 QQ
     *
     * @param event 消息事件
     * @return 发送者的 QQ
     */
    public static long getSenderIdAsLong(MessageEvent event) {
        return event.getSender().getId();
    }


    /**
     * 获取引用消息事件的源，如果不存在，则返回 null
     * <br>
     * 可以调用 {@link #equals(MessageSource, MessageSource)} 来比对两个消息是否是同一个消息
     *
     * @param event 事件
     * @return MessageSource 消息源
     */
    public static MessageSource getQuoteSource(MessageEvent event) {
        return Objects.requireNonNull(event.getMessage().get(QuoteReply.Key)).getSource();
    }

    /**
     * 设置引用回复，如果失败，则返回 null<br/>
     * 如果想回复某消息，你可以这样编写代码 <br>
     * <code><strong>buildMessageChain(getQuoteReply(e), msg)</strong></code>
     * 以构造一条消息链
     * <br/>或者使用 getQuoteReply 方法回复一条信息
     *
     * @param event 事件
     * @return MessageSource 引用回复
     * @see #buildMessageChain(Object...)
     * @see #quoteReply(MessageEvent, MessageChain...)
     */
    public static QuoteReply getQuoteReply(MessageEvent event) {
        return new QuoteReply(event.getMessage());
    }

    /**
     * 引用并回复一条消息
     *
     * @param event 引用的事件
     * @param msg   待回复的消息
     * @return 构造一条带有引用的消息回复
     */
    public static MessageChain quoteReply(MessageEvent event, Object... msg) {
        return buildMessageChain(getQuoteReply(event), msg);
    }

    /**
     * 引用回复一条消息
     *
     * @param event 引用的事件
     * @param msg   待回复的消息
     * @return 构造一条带有引用的消息回复
     */
    public static MessageChain quoteReply(MessageEvent event, MessageChain... msg) {
        return buildMessageChain(getQuoteReply(event), msg);
    }

    /**
     * 获取一个消息链中的 Images
     */
    public static List<Image> getImagesFromMessage(MessageChain chain) {
        return chain.stream().filter(Image.class::isInstance).map(v -> (Image) v).collect(Collectors.toList());
    }

    /**
     * 发送多条消息，<strong>注意此方法并不能保证发送消息的顺序<strong/>
     *
     * @param msg   消息链
     * @param event 发送对象
     */
    public static void sendMsg(List<MessageChain> msg, MessageEvent event) throws CanNotSendMessageException {
        sendMsg(msg, event.getSubject());
    }

    /**
     * 发送多条消息，<strong>注意此方法并不能保证发送消息的顺序<strong/>
     *
     * @param msg   消息链
     * @param event 发送对象
     */
    public static void sendMsg(MessageChain msg, MessageEvent event) throws CanNotSendMessageException {
        sendMsg(List.of(msg), event.getSubject());
    }

    /**
     * 发送多条消息，<strong>注意此方法并不能保证发送消息的顺序<strong/>
     *
     * @param msg     消息链
     * @param contact 发送对象
     */
    public static void sendMsg(List<MessageChain> msg, Contact contact) throws CanNotSendMessageException {
        try {
            for (MessageChain chain : msg) {
                contact.sendMessage(chain);
            }
        } catch (Exception e) {
            throw new CanNotSendMessageException(e.getMessage());
        }
    }


    /**
     * 异步发送多条消息，<strong>注意此方法并不能保证发送消息的顺序<strong/>
     *
     * @param msg     消息链
     * @param contact 发送对象
     */
    public static CompletableFuture<Void> sendMsgAsync(List<MessageChain> msg, Contact contact) throws CanNotSendMessageException {
        return CompletableFuture.runAsync(() -> {
            try {
                sendMsg(msg, contact);
            } catch (CanNotSendMessageException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 发送一条将自动撤回的消息
     *
     * @param msg             消息
     * @param contact         发送对象
     * @param autoRecallMills 自动撤回等待时间(毫秒)
     * @return
     */
    public static void sendMsg(MessageChain msg, Contact contact, long autoRecallMills) throws CanNotSendMessageException {
        try {
            contact.sendMessage(msg).recallIn(autoRecallMills);
        } catch (Exception e) {
            throw new CanNotSendMessageException(e.getMessage());
        }
    }

    /**
     * 发送一条将自动撤回的消息
     *
     * @param msg             消息
     * @param e               消息事件
     * @param autoRecallMills 自动撤回等待时间(毫秒)
     */
    public static void sendMsg(MessageChain msg, MessageEvent e, long autoRecallMills) throws CanNotSendMessageException {
        try {
            e.getSubject().sendMessage(msg).recallIn(autoRecallMills);
        } catch (Exception ee) {
            throw new CanNotSendMessageException(ee.getMessage());
        }
    }


    /**
     * 判断给定消息事件是否引用了一条消息
     *
     * @param event 消息事件
     * @return 真则代表引用了一条消息
     */
    public static boolean hasQuote(MessageEvent event) {
        return event.getMessage().get(QuoteReply.Key) != null;
    }

    /**
     * 从消息事件中取出事件所引用的消息
     * <p>这在某些情况下很有用，例如如果用户引用了某条消息来询问机器人，机器人可以获取用户引用的消息</p>
     *
     * @param event 消息事件
     * @return 引用的消息
     * @throws NullPointerException 如果消息事件不包含引用
     * @see #hasQuote(MessageEvent)
     */
    public static MessageChain getQuoteMessageChain(MessageEvent event) {
        return getQuoteSource(event).getOriginalMessage();
    }

    /**
     * 比较两个消息源是否为同一个源
     *
     * @param source1 1
     * @param source2 2
     * @return true if equals, else false
     */
    public static boolean equals(MessageSource source1, MessageSource source2) {
        if (source1 == null && source2 == null) return true;
        if (source1 == null || source2 == null) return false;
        return source1.getFromId() == source2.getFromId()
                && source1.getTargetId() == source2.getTargetId()
                && source1.getTime() == source2.getTime();
    }
}
