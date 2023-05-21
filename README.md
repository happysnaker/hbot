# HBot

<p align="center"> <img src="./img.png" width = "400" height = "300" alt="守护最好的 kunkun称" align=center /></p>

<p align='center'>
<img src="https://img.shields.io/badge/style-Modern Java Style-brightgreen.svg">
<img src="https://img.shields.io/badge/platform-%20WINDOWS | MAC | LINUX%20-ff69b4.svg">
<img src="https://img.shields.io/badge/language-JAVA-orange.svg">
<img src="https://img.shields.io/badge/Author-Happysnaker-green.svg">
</p>

HBot 出生于 [HRobot](https://github.com/happysnaker/mirai-plugin-HRobot)，是一款整合 [Mirai | mirai](https://mamoe.github.io/mirai/) 与 SpringBoot 的 **QQ 群机器人**开发框架，Java 开发人员可使用 Maven 快速整合 HBot 以开发属于你自己的 QQ 群机器人，或以 HBot-Plugin 提供全量服务，亦或以 [MCL一键安装工具](https://github.com/iTXTech/mcl-installer) 插件的形式发布到 Mirai 社区。

> 做 HBot 这个项目的原因有几点，首要的原因是当年半小时写了个 [HRobot](https://github.com/happysnaker/mirai-plugin-HRobot)，
> 没想但后来越写越杂、越写越乱，难以维护，因此抽离出其核心框架，HBot 因此而来。
> 
> 其次 Mirai 生态很完善，但是一直没有纯 Java 编写的 SpringBoot 框架，Java 选手需要下载很多额外的东西，例如 [mirai-console](https://docs.mirai.mamoe.net/console/) 就需要安装 mcl 下载器，编写插件运行。
> 
> 当然 [simbot](https://github.com/simple-robot/simpler-robot) 是一个非常优秀的项目，但是其源代码仍然是由 Kotlin 编写的，而且有点重，Java 不友好。
> 
> HBot 是纯 Java 编写的，使用 jdk17，完全现代化的 Java 风格。HBot 只专注群聊事件，是一个纯群聊机器人，以最小的依赖尽可能的提供更完善的功能。整个项目非常轻量，源代码仅有 4000 行左右，仅 200kb 不到。
> 
> 这个项目是工作闲暇之余捣鼓的，如果能够帮助到各位 Gay 友，那么我认为这件事情真是泰裤辣！

<p align="center"><a href="https://www.yuque.com/anywhyobjdumpdbooto/rs18r6/pwzzkei38si0uga5">使用文档</a></p>


## 特性

- **基于 JDK17 与 SpringBoot3.x**。HBot 完全基于 Java 开发，现代化 Java 风格，使用稳定版本 JDK17，轻量级，Java 友好。
- **简单高效**。HBot 是一款基于消息事件的自动调度框架，使得开发人员专注与消息应答逻辑，简单易用，扩展性强。
- **自动化的配置处理方式**。HBot 能够自动从配置文件中读取配置到程序的配置类中，供开发人员直接使用。
- **可引入其他 HBot 应用**。HBot 开发出来的应用可互相引用，功能拓展十分方便。
- **适配多种场景发布**。HBot 可发布 Jar 供他人使用，也可作为 HBot-Plugin 向其他 Hbot 提供全量服务，同时，HBot 亦天然适配 [MCL Plugin](https://github.com/iTXTech/mcl-installer) 开发。
- **完善的命令处理机制与权限管理**。HBot 有着完善的命令处理机制与权限管理，可以使你的机器人更加健壮安全。
- **可定制化的定时调度事件**。HBot 基于 quartz 组件提供了定时调度任务的解决方案，使用 HBot 你可以便捷的在未来的某个时间点定时或者定期向一些群推送消息。
- **动态标签**。HBot 能够解析一些具有动态语义的编码，例如 `[hrobot::text](http:xxxx)` 在解码时 HBot 会自动向对应地址请求一段文字。 
- **支持长对话**。HBot 提供了对长对话的同步与异步 API 支持。
- **内置轻量级逆向工程网页版 ChatGPT**。HBot 以 Java 源码的形式内置了当下最火热逆向工程版的 [ChatGPT](https://github.com/Pumpkin9841/Chatgpt-java)，使用者无需翻墙、无需付费即可快速搭建 ChatGPT 机器人。
<details><summary><h2>快速使用</h2></summary>

1. **安装**

maven 引入坐标：
```xml
<dependency>
    <groupId>io.github.happysnaker</groupId>
    <artifactId>hbot-core</artifactId>
    <version>${version}</version>
</dependency>
```

2. **编写处理器**

```java
@handler
@InterestFilters(value = {
        @InterestFilter(mode = Interest.MODE.REGEX, condition = ".*鸡汤.*", output = "[hrobot::$quote](quote)[hrobot::$text](https://api.qinor.cn/soup/)"),
        @InterestFilter(mode = Interest.MODE.REGEX, condition = "早.+", output = "[hrobot::$at](sender)早早早，早上好！")
})
public class InterestHandler extends AdaptInterestMessageEventHandler {
}
```

3. **启动**

```java
@SpringBootApplication
@EnableHBot
public class HBotDemoApplication {
    static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        Thread.currentThread().setContextClassLoader(PluginClassLoader.instance);
        SpringApplication.run(HBotDemoApplication.class, args);
        HBot.loginBotByQRCode(123456, BotConfiguration.MiraiProtocol.ANDROID_WATCH);
    }
}
```

更多使用方式请参考：[使用文档](https://www.yuque.com/anywhyobjdumpdbooto/rs18r6/pwzzkei38si0uga5)

</details>


## 鸣谢
- [Mirai 高效率机器人支持库](https://github.com/mamoe/mirai)
- [Java 版 RevChatGpt](https://github.com/Pumpkin9841/Chatgpt-java)
- [ChatGpt 逆向工程](https://github.com/acheong08/ChatGPT)






