package com.huaban.analysis.jieba;

/**
 * 分词结果单元
 * 功能：
 * 1. 存储分词后的词语及其位置信息
 * 2. 支持结果的可视化输出
 * 3. 为后续处理提供位置上下文
 * 
 * 设计要点：
 * - 不可变对象（线程安全）
 * - 精确记录字符位置
 * - 支持多种输出格式
 */
public class SegToken {
    // 使用final保证不可变性
    public final String word;          // 词语内容（不可变）
    public final int startOffset;      // 起始位置（包含）
    public final int endOffset;        // 结束位置（不包含）

    /**
     * 构造函数
     * @param word 词语内容
     * @param start 起始位置（从0开始）
     * @param end 结束位置（exclusive）
     */
    public SegToken(String word, int start, int end) {
        this.word = word;
        this.startOffset = start;
        this.endOffset = end;
    }

    /**
     * 获取词语长度
     * @return endOffset - startOffset
     */
    public int length() {
        return endOffset - startOffset;
    }

    /**
     * 格式化输出
     * @return "[词语, 起始位置, 结束位置]"
     */
    @Override
    public String toString() {
        return String.format("[%s, %d, %d]", word, startOffset, endOffset);
    }
}
