package com.huaban.analysis.jieba;

/**
 * @description: enable output content to be controlled by switch
 * @author: sharkdoodoo@foxmail.com
 * @date: 2022/6/21
 */
public class Log {

    private static final boolean LOG_ENABLE = Boolean.parseBoolean(System.getProperty("jieba.log.enable", "true"));

    public static final void debug(String debugInfo) {
        if (LOG_ENABLE) {
            System.out.println(debugInfo);
        }
    }

    public static final void error(String errorInfo) {
        if (LOG_ENABLE) {
            System.err.println(errorInfo);
        }
    }
}

/**
 * 统一日志管理
 * 功能：
 * - 控制日志输出级别
 * - 格式化日志信息
 * - 异常处理
 * 配置建议：
 * 生产环境关闭DEBUG日志
 */
