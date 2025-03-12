package com.huaban.analysis.jieba;

import java.util.regex.Pattern;

/**
 * 字符处理工具类
 * 功能：
 * 1. 字符类型判断（中文、英文、数字、连接符）
 * 2. 字符规范化处理（全角转半角、大写转小写）
 * 3. 提供通用字符匹配规则
 */
public class CharacterUtil {
    // 匹配需要跳过的字符模式（数字、字母等组合）
    public static Pattern reSkip = Pattern.compile("(\\d+\\.\\d+|[a-zA-Z0-9]+)");
    
    // 常见连接符号集合
    private static final char[] connectors = new char[] { '+', '#', '&', '.', '_', '-' };

    /**
     * 判断是否为中文字符（包含CJK统一汉字）
     * @param ch 待判断字符
     * @return true-是中文，false-非中文
     */
    public static boolean isChineseLetter(char ch) {
        // 0x4E00-0x9FA5是CJK统一汉字基本集范围
        return ch >= 0x4E00 && ch <= 0x9FA5;
    }

    /**
     * 判断是否为英文字母
     * @param ch 待判断字符
     * @return true-是英文字母，false-非英文
     */
    public static boolean isEnglishLetter(char ch) {
        // 0x0041-0x005A是A-Z，0x0061-0x007A是a-z
        return (ch >= 0x0041 && ch <= 0x005A) || 
               (ch >= 0x0061 && ch <= 0x007A);
    }

    /**
     * 判断是否为数字字符
     * @param ch 待判断字符
     * @return true-是数字，false-非数字
     */
    public static boolean isDigit(char ch) {
        // 0x0030-0x0039对应0-9
        return ch >= 0x0030 && ch <= 0x0039;
    }

    /**
     * 判断是否为连接符号
     * @param ch 待判断字符
     * @return true-是连接符，false-非连接符
     */
    public static boolean isConnector(char ch) {
        // 遍历预定义连接符集合进行匹配
        for (char connector : connectors) {
            if (ch == connector)
                return true;
        }
        return false;
    }

    /**
     * 综合字符判断（中文/英文/数字/连接符）
     * @param ch 待判断字符
     * @return true-是以上任意类型，false-其他字符
     */
    public static boolean ccFind(char ch) {
        return isChineseLetter(ch) || 
               isEnglishLetter(ch) || 
               isDigit(ch) || 
               isConnector(ch);
    }

    /**
     * 字符规范化处理：
     * 1. 全角空格转半角
     * 2. 全角字符转半角
     * 3. 大写字母转小写
     * @param input 原始字符
     * @return 规范化后的字符
     */
    public static char regularize(char input) {
        // 处理全角空格（12288对应全角空格，32是半角空格）
        if (input == 12288) {
            return 32;
        }
        // 处理全角字符（65281-65374对应全角字符，转换为半角需减去65248）
        else if (input > 65280 && input < 65375) {
            return (char) (input - 65248);
        }
        // 处理大写字母（A-Z转a-z）
        else if (input >= 'A' && input <= 'Z') {
            return (char) (input + 32);
        }
        return input;
    }
}
