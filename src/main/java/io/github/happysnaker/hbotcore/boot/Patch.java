package io.github.happysnaker.hbotcore.boot;




import java.io.File;
import java.util.Objects;

/**
 *
 * @author Happysnaker
 * @description
 * @date 2022/1/16
 * @email happysnaker@foxmail.com
 */
public class Patch {
    public static final String fileName = "account.secrets";

    private static void dfs(File file) {
        if (file.isDirectory()) {
            for (File listFile : Objects.requireNonNull(file.listFiles())) {
                dfs(listFile);
            }
        } else {
            if (file.isFile() && file.getName().contains(fileName)) {
                boolean delete = file.delete();
            }
        }
    }

    public static void patch() {
        // 某些时候群聊消息发不出去，需要删除 account.secrets 文件夹
        File file = new File(HBot.BOT_DIR);
        if (file.isDirectory()) {
            for (File listFile : Objects.requireNonNull(file.listFiles())) {
                if (listFile.isDirectory()) {
                    dfs(listFile);
                    break;
                }
            }
        }
    }
}