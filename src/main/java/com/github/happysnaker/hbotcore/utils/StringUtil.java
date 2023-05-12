package com.github.happysnaker.hbotcore.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Happysnaker
 * @description
 * @date 2022/2/22
 * @email happysnaker@foxmail.com
 */
public class StringUtil {

    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    public static List<String> splitSpaces(String s) {
        List<String> ans = new ArrayList<>();
        for (String s1 : s.split("\\s+")) {
            if (!s1.isEmpty()) {
                ans.add(s1);
            }
        }
        return ans;
    }

    public static String reverse(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = str.length() - 1; i >= 0; i--) {
            sb.append(str.charAt(i));
        }
        return sb.toString();
    }

    /**
     * 获取编辑距离，值越小，二者越相似
     *
     * @param word1
     * @param word2
     * @return
     */
    public static int getEditDistance(String word1, String word2) {
        int n1 = word1.length(), n2 = word2.length();
        int[][] dp = new int[n1 + 1][n2 + 1];
        for (int j = 1; j <= n2; j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= n1; i++) {
            dp[i][0] = i;
        }
        for (int i = 1; i <= n1; i++) {
            for (int j = 1; j <= n2; j++) {
                dp[i][j] = Math.min(dp[i][j - 1] + 1, dp[i - 1][j] + 1);
                if (word1.charAt(i - 1) != word2.charAt(j - 1)) {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 1][j - 1] + 1);
                } else {
                    dp[i][j] = Math.min(dp[i][j], dp[i - 1][j - 1]);
                }
            }
        }
        return dp[n1][n2];
    }

    public static String getErrorInfoFromException(Throwable e) {
        try {
            StringWriter sw = null;
            PrintWriter pw = null;
            try {
                sw = new StringWriter();
                pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                return sw.toString();
            } finally {
                assert sw != null;
                assert pw != null;
                sw.close();
                pw.close();
            }
        } catch (Exception e1) {
            return e.getMessage();
        }
    }
}
