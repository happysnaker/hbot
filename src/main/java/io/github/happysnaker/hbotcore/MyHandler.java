package io.github.happysnaker.hbotcore;

import io.github.happysnaker.hbotcore.command.AdaptInterestCommandEventHandler;
import io.github.happysnaker.hbotcore.exception.CanNotParseCommandException;
import io.github.happysnaker.hbotcore.exception.InsufficientPermissionsException;
import io.github.happysnaker.hbotcore.handler.*;
import io.github.happysnaker.hbotcore.permisson.Permission;
import io.github.happysnaker.hbotcore.proxy.Context;
import net.mamoe.mirai.event.events.GroupMessageEvent;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.MusicKind;
import net.mamoe.mirai.message.data.MusicShare;

import java.util.List;

/**
 * @Author happysnaker
 * @Date 2023/5/20
 * @Email happysnaker@foxmail.com
 */
//@handler(priority = 64)
@InterestFilters(value = {
        @InterestFilter(mode = Interest.MODE.PREFIX, condition = "音乐", callbackMethod = "searchMusic"),
        @InterestFilter(mode = Interest.MODE.REGEX, condition = "牛.+", output = "[hrobot::$quote]()怎么牛？")
})
public class MyHandler extends AdaptInterestCommandEventHandler{

    @Override
    public List<MessageChain> parseCommand(GroupMessageEvent event, Context context) throws CanNotParseCommandException, InsufficientPermissionsException {
        return super.parseCommand(event, context);
    }

    public MessageChain searchMusic(Interest.DispatchArgs args, GroupMessageEvent event, Context ctx) {
        String music = getPlantContent(event).replaceFirst(args.getCondition(), "");
        String jumpUrl = "https://baidu.com";
        String pictureUrl = "https://p2.music.126.net/y19E5SadGUmSR8SZxkrNtw==/109951163785855539.jpg";
        String songUrl = "your music url";
        return buildMessageChain(new MusicShare(
                MusicKind.QQMusic, music, "您搜索的音乐是" + music,
                jumpUrl, pictureUrl, songUrl
        ));
    }
}