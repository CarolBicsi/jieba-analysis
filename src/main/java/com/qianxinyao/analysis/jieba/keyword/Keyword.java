package com.qianxinyao.analysis.jieba.keyword;

/**
 * 关键词提取核心类（基于TF-IDF算法）
 * 功能：
 * 1. 存储关键词及其TF-IDF权重
 * 2. 实现关键词的排序比较
 * 3. 提供关键词对象的基本操作
 * 
 * 核心成员：
 * - name：关键词文本
 * - tfidfvalue：TF-IDF权重值（保留4位小数）
 * 
 * 设计特点：
 * - 实现Comparable接口支持排序
 * - 重写equals/hashCode方法确保对象唯一性
 * - 提供标准的JavaBean访问方法
 */
public class Keyword implements Comparable<Keyword> {
    
    // TF-IDF权重值
    private double tfidfvalue;
    
    // 关键词文本
    private String name;

    /**
     * 获取TF-IDF值
     */
    public double getTfidfvalue() {
        return tfidfvalue;
    }

    /**
     * 设置TF-IDF值（自动保留4位小数）
     */
    public void setTfidfvalue(double tfidfvalue) {
        this.tfidfvalue = (double) Math.round(tfidfvalue * 10000) / 10000;
    }

    /**
     * 获取关键词文本
     */
    public String getName() {
        return name;
    }

    /**
     * 设置关键词文本
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 构造函数
     * @param name 关键词文本
     * @param tfidfvalue 原始TF-IDF值
     */
    public Keyword(String name, double tfidfvalue) {
        this.name = name;
        this.tfidfvalue = (double) Math.round(tfidfvalue * 10000) / 10000;
    }

    /**
     * 比较方法（降序排列）
     */
    @Override
    public int compareTo(Keyword o) {
        return Double.compare(o.tfidfvalue, this.tfidfvalue);
    }

    /**
     * 重写equals方法（基于name和tfidfvalue）
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Keyword other = (Keyword) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    /**
     * 重写hashCode方法（基于name）
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
}

