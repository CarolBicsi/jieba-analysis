package com.huaban.analysis.jieba;

/**
 * 词典匹配结果记录器
 * 功能：
 * 1. 记录词典匹配状态（未匹配/完全匹配/前缀匹配）
 * 2. 保存匹配位置信息
 * 3. 维护当前匹配的字典树节点
 * 
 * 状态说明：
 * - 未匹配：当前路径不在词典中
 * - 完全匹配：找到完整词语
 * - 前缀匹配：当前路径是某词语的前缀
 */
public class Hit {
	// 状态标志位（使用位运算提高效率）
	private static final int UNMATCH = 0x00000000;  // 0000 未匹配
	private static final int MATCH   = 0x00000001;  // 0001 完全匹配
	private static final int PREFIX  = 0x00000010;  // 0010 前缀匹配
	
	// 当前匹配状态（默认未匹配）
	private int hitState = UNMATCH;
	
	// 匹配到的字典树节点（用于前缀匹配继续查找）
	private DictSegment matchedDictSegment;
	
	// 匹配位置信息
	private int begin;  // 匹配起始位置
	private int end;    // 匹配结束位置
	
	/**
	 * 判断是否完全匹配
	 * @return true-找到完整词语
	 */
	public boolean isMatch() {
		return (this.hitState & MATCH) > 0;
	}
	/**
	 * 设置完全匹配状态
	 */
	public void setMatch() {
		this.hitState |= MATCH; // 按位或操作设置标志位
	}

	/**
	 * 判断是否是前缀匹配
	 * @return true-当前路径是某词语前缀
	 */
	public boolean isPrefix() {
		return (this.hitState & PREFIX) > 0;
	}
	/**
	 * 设置前缀匹配状态
	 */
	public void setPrefix() {
		this.hitState |= PREFIX;
	}
	/**
	 * 判断是否未匹配
	 * @return true-完全未匹配
	 */
	public boolean isUnmatch() {
		return this.hitState == UNMATCH;
	}
	/**
	 * 重置为未匹配状态
	 */
	public void setUnmatch() {
		this.hitState = UNMATCH;
	}
	
	/**
	 * 获取匹配的字典树节点
	 * @return 当前匹配的节点（可能为null）
	 */
	public DictSegment getMatchedDictSegment() {
		return matchedDictSegment;
	}
	
	/**
	 * 设置当前匹配的字典树节点
	 * @param matchedDictSegment 字典树节点
	 */
	public void setMatchedDictSegment(DictSegment matchedDictSegment) {
		this.matchedDictSegment = matchedDictSegment;
	}
	
	/**
	 * 获取匹配起始位置
	 * @return 起始索引
	 */
	public int getBegin() {
		return begin;
	}
	
	/**
	 * 设置匹配起始位置
	 * @param begin 起始索引
	 */
	public void setBegin(int begin) {
		this.begin = begin;
	}
	
	/**
	 * 获取匹配结束位置
	 * @return 结束索引（exclusive）
	 */
	public int getEnd() {
		return end;
	}
	
	/**
	 * 设置匹配结束位置
	 * @param end 结束索引（exclusive）
	 */
	public void setEnd(int end) {
		this.end = end;
	}	
	
}
