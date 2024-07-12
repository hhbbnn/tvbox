package com.github.tvbox.osc.util;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

    /**
     * 去除字符串中的所有符号和特殊符号，并替换成空格
     *
     * @param input 原始字符串
     * @return 处理后的字符串
     */
    public static String replaceSymbolsWithSpace(String input) {
        // 正则表达式，匹配除字母、数字和中文以外的所有字符
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5]");
        Matcher matcher = pattern.matcher(input);
        // 使用replaceAll方法替换匹配到的字符为一个空格
        return matcher.replaceAll(" ").trim();
    }

    /**
     * 去除字符串中的所有符号和特殊符号，并替换成·
     *
     * @param input 原始字符串
     * @return 处理后的字符串
     */
    public static String replaceSymbolsWithPoint(String input) {
        // 正则表达式，匹配除字母、数字和中文以外的所有字符
        Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5]");
        Matcher matcher = pattern.matcher(input);
        // 使用replaceAll方法替换匹配到的字符为一个空格
        return matcher.replaceAll(" ").trim().replaceAll(" ", "·");
    }

    private static final String U2028 = new String(new byte[]{(byte) 0xE2, (byte) 0x80, (byte) 0xA8});
    private static final String U2029 = new String(new byte[]{(byte) 0xE2, (byte) 0x80, (byte) 0xA9});

    /**
     * Escape JavaString string
     *
     * @param line unescaped string
     * @return escaped string
     */
    public static String escapeJavaScriptString(final String line) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            switch (c) {
                case '"':
                case '\'':
                case '\\':
                    sb.append('\\');
                    sb.append(c);
                    break;

                case '\n':
                    sb.append("\\n");
                    break;

                case '\r':
                    sb.append("\\r");
                    break;

                default:
                    sb.append(c);
            }
        }

        return sb.toString()
                .replace(U2028, "\u2028")
                .replace(U2029, "\u2029");
    }

    public static String getBaseUrl(String url) {
        if (org.apache.commons.lang3.StringUtils.isBlank(url)) {
            return url;
        }

        String baseUrls = url.replace("http://", "").replace("https://", "");
        String baseUrl2 = baseUrls.split("/")[0];
        String baseUrl;
        if (url.startsWith("https")) {
            baseUrl = "https://" + baseUrl2;
        } else {
            baseUrl = "http://" + baseUrl2;
        }
        return baseUrl;
    }
}