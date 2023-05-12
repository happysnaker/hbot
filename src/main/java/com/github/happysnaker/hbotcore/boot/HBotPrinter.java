package com.github.happysnaker.hbotcore.boot;


/**
 * @author Happysnaker
 * @description
 * @date 2022/2/27
 * @email happysnaker@foxmail.com
 */
public class HBotPrinter {
    static String banner = """
                      ,--,                                \s
                    ,--.'|    ,---,.               ___    \s
                 ,--,  | :  ,'  .'  \\            ,--.'|_  \s
              ,---.'|  : ',---.' .' |   ,---.    |  | :,' \s
              |   | : _' ||   |  |: |  '   ,'\\   :  : ' : \s
              :   : |.'  |:   :  :  / /   /   |.;__,'  /  \s
              |   ' '  ; ::   |    ; .   ; ,. :|  |   |   \s
              '   |  .'. ||   :     \\'   | |: ::__,'| :   \s
              |   | :  | '|   |   . |'   | .; :  '  : |__ \s
              '   : |  : ;'   :  '; ||   :    |  |  | '.'|\s
              |   | '  ,/ |   |  | ;  \\   \\  /   ;  :    ;\s
              ;   : ;--'  |   :   /    `----'    |  ,   / \s
              |   ,/      |   | ,'                ---`-'  \s
              '---'       `----'                           v1.0.0\s    
            """;

    public static void printBanner() {
        System.out.println(banner);
    }
}
