package com.huaban.analysis.jieba;

/**
 * 维特比路径节点
 * 组成：
 * - 当前状态（B/M/E/S）
 * - 前驱节点指针
 * 核心作用：
 * 回溯生成最优状态序列
 * 内存优化：
 * 仅存储必要信息，避免对象膨胀
 */
public class Node {
    public final Character value;  // 当前状态（B/M/E/S）
    public final Node parent;      // 前驱状态节点

    /**
     * 构造函数
     * @param value 当前状态字符
     * @param parent 前驱节点（可能为null）
     */
    public Node(Character value, Node parent) {
        this.value = value;
        this.parent = parent;
    }
}
