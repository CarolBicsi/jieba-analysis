package com.qianxinyao.analysis.jieba.keyword;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.huaban.analysis.jieba.JiebaSegmenter;

/**
 * @author Tom Qian
 * @email tomqianmaple@outlook.com
 * @github https://github.com/bluemapleman
 * @date Oct 20, 2018
 * tfidf算法原理参考：http://www.cnblogs.com/ywl925/p/3275878.html
 * 部分实现思路参考jieba分词：https://github.com/fxsjy/jieba
 * 
 * TF-IDF关键词提取器
 * 实现思路：
 * 1. 预处理：
 *    - 加载停用词表（stop_words.txt）过滤无意义词汇
 *    - 加载IDF字典（idf_dict.txt）获取全局逆文档频率
 * 2. 文本处理：
 *    - 使用结巴分词进行中文分词
 *    - 过滤单字词和停用词
 * 3. 特征计算：
 *    - 计算词频TF（Term Frequency）
 *    - 结合IDF值计算TF-IDF权重
 * 4. 结果处理：
 *    - 按TF-IDF值降序排序
 *    - 截取topN个关键词
 */
public class TFIDFAnalyzer
{
	// IDF值字典（词 -> IDF值）
	static HashMap<String,Double> idfMap;
	// 停用词集合
	static HashSet<String> stopWordsSet;
	// IDF中位数值（用于处理未登录词）
	static double idfMedian;
	
	/**
	 * 核心分析方法
	 * @param content 待分析文本
	 * @param topN 返回关键词数量
	 * @return 关键词列表（按TF-IDF降序）
	 * 
	 * 实现步骤：
	 * 1. 初始化资源（首次运行时加载）
	 * 2. 计算TF值
	 * 3. 计算TF-IDF值（未登录词使用IDF中位数）
	 * 4. 排序并截取topN结果
	 */
	public List<Keyword> analyze(String content,int topN){
		List<Keyword> keywordList=new ArrayList<>();
		
		if(stopWordsSet==null) {
			stopWordsSet=new HashSet<>();
			loadStopWords(stopWordsSet, this.getClass().getResourceAsStream("/stop_words.txt"));
		}
		if(idfMap==null) {
			idfMap=new HashMap<>();
			loadIDFMap(idfMap, this.getClass().getResourceAsStream("/idf_dict.txt"));
		}
		
		Map<String, Double> tfMap=getTF(content);
		for(String word:tfMap.keySet()) {
			// 若该词不在idf文档中，则使用平均的idf值(可能定期需要对新出现的网络词语进行纳入)
			if(idfMap.containsKey(word)) {
				keywordList.add(new Keyword(word,idfMap.get(word)*tfMap.get(word)));
			}else
				keywordList.add(new Keyword(word,idfMedian*tfMap.get(word)));
		}
		
		Collections.sort(keywordList);
		
		if(keywordList.size()>topN) {
			int num=keywordList.size()-topN;
			for(int i=0;i<num;i++) {
				keywordList.remove(topN);
			}
		}
		return keywordList;
	}
	
	/**
	 * 计算词频TF（Term Frequency）
	 * 公式：TF = 该词在文档中出现次数 / 文档总词数
	 * @param content 文本内容
	 * @return 词频字典（词 -> TF值）
	 * 
	 * 处理流程：
	 * 1. 结巴分词
	 * 2. 过滤停用词和单字词
	 * 3. 统计词频
	 * 4. 计算归一化TF值（乘以0.1进行缩放）
	 */
	private Map<String, Double> getTF(String content) {
		Map<String,Double> tfMap=new HashMap<>();
		if(content==null || content.equals(""))
			return tfMap; 
		
		JiebaSegmenter segmenter = new JiebaSegmenter();
		List<String> segments=segmenter.sentenceProcess(content);
		Map<String,Integer> freqMap=new HashMap<>();
		
		int wordSum=0;
		for(String segment:segments) {
			//停用词不予考虑，单字词不予考虑
			if(!stopWordsSet.contains(segment) && segment.length()>1) {
				wordSum++;
				if(freqMap.containsKey(segment)) {
					freqMap.put(segment,freqMap.get(segment)+1);
				}else {
					freqMap.put(segment, 1);
				}
			}
		}
		
		// 计算double型的tf值
		for(String word:freqMap.keySet()) {
			tfMap.put(word,freqMap.get(word)*0.1/wordSum);
		}
		
		return tfMap; 
	}
	
	/**
	 * 加载停用词表
	 * @param set 停用词集合
	 * @param in 停用词表输入流
	 * 注：使用默认结巴停用词表，包含常见无意义词汇
	 */
	private void loadStopWords(Set<String> set, InputStream in){
		BufferedReader bufr;
		try
		{
			bufr = new BufferedReader(new InputStreamReader(in));
			String line=null;
			while((line=bufr.readLine())!=null) {
				set.add(line.trim());
			}
			try
			{
				bufr.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * 加载IDF字典
	 * @param map IDF字典
	 * @param in IDF文件输入流
	 * 说明：
	 * - 字典格式：词语 + 空格 + IDF值
	 * - 计算IDF中位数用于处理未登录词
	 */
	private void loadIDFMap(Map<String,Double> map, InputStream in ){
		BufferedReader bufr;
		try
		{
			bufr = new BufferedReader(new InputStreamReader(in));
			String line=null;
			while((line=bufr.readLine())!=null) {
				String[] kv=line.trim().split(" ");
				map.put(kv[0],Double.parseDouble(kv[1]));
			}
			try
			{
				bufr.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			
			// 计算idf值的中位数
			List<Double> idfList=new ArrayList<>(map.values());
			Collections.sort(idfList);
			idfMedian=idfList.get(idfList.size()/2);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args)
	{
		String content="孩子上了幼儿园 安全防拐教育要做好";
		int topN=5;
		TFIDFAnalyzer tfidfAnalyzer=new TFIDFAnalyzer();
		List<Keyword> list=tfidfAnalyzer.analyze(content,topN);
		for(Keyword word:list)
			System.out.print(word.getName()+":"+word.getTfidfvalue()+",");
	}
}

