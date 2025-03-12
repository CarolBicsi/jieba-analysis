package com.huaban.analysis.jieba;

/**
 * 通用键值对容器（带权重）
 * 功能：
 * 1. 存储候选词及其权重值（概率值）
 * 2. 用于动态规划中的路径选择
 * 3. 支持结果排序和调试输出
 * 
 * 设计特点：
 * - 泛型设计支持多种键类型
 * - 使用Double存储对数概率值
 * - 轻量级数据结构（仅8字节开销）
 * 
 * 应用场景：
 * - DAG图中的边权重存储
 * - 维特比算法中的路径选择
 * - 候选词概率排序
 */
public class Pair<K> {
    // 键值对元素
    public K key;        // 候选词/位置标识等
    public Double freq;  // 权重值（使用对数概率避免浮点溢出）

    /**
     * 构造函数
     * @param key 候选键（如结束位置、词语等）
     * @param freq 初始权重值（建议使用对数概率）
     */
    public Pair(K key, double freq) {
	this.key = key;
	this.freq = freq;
    }

    /**
     * 调试用字符串表示
     * @return 包含键和权重的可读字符串
     */
    @Override
    public String toString() {
	return "Candidate [key=" + key + ", freq=" + freq + "]";
    }

}
