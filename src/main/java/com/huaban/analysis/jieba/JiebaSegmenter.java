package com.huaban.analysis.jieba;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.huaban.analysis.jieba.viterbi.FinalSeg;

/**
 * 结巴分词核心入口类
 * 功能：提供多种分词模式，协调词典匹配和HMM模型处理
 * 实现原理：
 * 1. 基于词典构建DAG（有向无环图）
 * 2. 动态规划计算最大概率路径
 * 3. 结合HMM模型处理未登录词
 */
public class JiebaSegmenter {
    // 单例词典实例（保持原有代码不变）
    private static WordDictionary wordDict = WordDictionary.getInstance();
    private static FinalSeg finalSeg = FinalSeg.getInstance();

    /**
     * 分词模式枚举：
     * INDEX - 全模式，输出所有可能成词的结果
     * SEARCH - 精确模式，按最细粒度切分
     */
    public static enum SegMode {
        INDEX,  // 示例："北京大学" → ["北京","北京大学","大学"]
        SEARCH  // 示例："北京大学" → ["北京","大学"]
    }

    /**
     * 初始化用户词典（保持原有代码不变）
     */
    public void initUserDict(Path path){
        wordDict.init(path);
    }

    public void initUserDict(String[] paths){
        wordDict.init(paths);
    }

    /**
     * 创建DAG有向无环图（保持原有代码结构）
     * 实现步骤：
     * 1. 使用Trie树进行前缀匹配
     * 2. 记录所有可能的词路径
     * 3. 保证每个位置至少有一个节点（单字）
     */
    private Map<Integer, List<Integer>> createDAG(String sentence) {
        Map<Integer, List<Integer>> dag = new HashMap<Integer, List<Integer>>();
        DictSegment trie = wordDict.getTrie();
        char[] chars = sentence.toCharArray();
        int N = chars.length;
        int i = 0, j = 0;
        while (i < N) {
            Hit hit = trie.match(chars, i, j - i + 1);
            if (hit.isPrefix() || hit.isMatch()) {
                if (hit.isMatch()) {
                    if (!dag.containsKey(i)) {
                        List<Integer> value = new ArrayList<Integer>();
                        dag.put(i, value);
                        value.add(j);
                    }
                    else
                        dag.get(i).add(j);
                }
                j += 1;
                if (j >= N) {
                    i += 1;
                    j = i;
                }
            }
            else {
                i += 1;
                j = i;
            }
        }
        for (i = 0; i < N; ++i) {
            if (!dag.containsKey(i)) {
                List<Integer> value = new ArrayList<Integer>();
                value.add(i);
                dag.put(i, value);
            }
        }
        return dag;
    }

    /**
     * 动态规划计算最优路径（保持原有代码结构）
     * 算法特点：
     * - 逆序计算：从右向左进行DP
     * - 概率累加：当前词概率 + 后续路径概率
     */
    private Map<Integer, Pair<Integer>> calc(String sentence, Map<Integer, List<Integer>> dag) {
        int N = sentence.length();
        HashMap<Integer, Pair<Integer>> route = new HashMap<Integer, Pair<Integer>>();
        route.put(N, new Pair<Integer>(0, 0.0));
        for (int i = N - 1; i > -1; i--) {
            Pair<Integer> candidate = null;
            for (Integer x : dag.get(i)) {
                double freq = wordDict.getFreq(sentence.substring(i, x + 1)) + route.get(x + 1).freq;
                if (null == candidate) {
                    candidate = new Pair<Integer>(x, freq);
                }
                else if (candidate.freq < freq) {
                    candidate.freq = freq;
                    candidate.key = x;
                }
            }
            route.put(i, candidate);
        }
        return route;
    }

    /**
     * 处理整段文本的分词（保持原有代码结构）
     * 处理流程：
     * 1. 分离中文字符和非中文字符
     * 2. 中文部分使用DAG+DP处理
     * 3. 非中文部分按规则切分
     */
    public List<SegToken> process(String paragraph, SegMode mode) {
        List<SegToken> tokens = new ArrayList<SegToken>();
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        for (int i = 0; i < paragraph.length(); ++i) {
            char ch = CharacterUtil.regularize(paragraph.charAt(i));
            if (CharacterUtil.ccFind(ch))
                sb.append(ch);
            else {
                if (sb.length() > 0) {
                    // process
                    if (mode == SegMode.SEARCH) {
                        for (String word : sentenceProcess(sb.toString())) {
                            tokens.add(new SegToken(word, offset, offset += word.length()));
                        }
                    }
                    else {
                        for (String token : sentenceProcess(sb.toString())) {
                            if (token.length() > 2) {
                                String gram2;
                                int j = 0;
                                for (; j < token.length() - 1; ++j) {
                                    gram2 = token.substring(j, j + 2);
                                    if (wordDict.containsWord(gram2))
                                        tokens.add(new SegToken(gram2, offset + j, offset + j + 2));
                                }
                            }
                            if (token.length() > 3) {
                                String gram3;
                                int j = 0;
                                for (; j < token.length() - 2; ++j) {
                                    gram3 = token.substring(j, j + 3);
                                    if (wordDict.containsWord(gram3))
                                        tokens.add(new SegToken(gram3, offset + j, offset + j + 3));
                                }
                            }
                            tokens.add(new SegToken(token, offset, offset += token.length()));
                        }
                    }
                    sb = new StringBuilder();
                    offset = i;
                }
                if (wordDict.containsWord(paragraph.substring(i, i + 1)))
                    tokens.add(new SegToken(paragraph.substring(i, i + 1), offset, ++offset));
                else
                    tokens.add(new SegToken(paragraph.substring(i, i + 1), offset, ++offset));
            }
        }
        if (sb.length() > 0)
            if (mode == SegMode.SEARCH) {
                for (String token : sentenceProcess(sb.toString())) {
                    tokens.add(new SegToken(token, offset, offset += token.length()));
                }
            }
            else {
                for (String token : sentenceProcess(sb.toString())) {
                    if (token.length() > 2) {
                        String gram2;
                        int j = 0;
                        for (; j < token.length() - 1; ++j) {
                            gram2 = token.substring(j, j + 2);
                            if (wordDict.containsWord(gram2))
                                tokens.add(new SegToken(gram2, offset + j, offset + j + 2));
                        }
                    }
                    if (token.length() > 3) {
                        String gram3;
                        int j = 0;
                        for (; j < token.length() - 2; ++j) {
                            gram3 = token.substring(j, j + 3);
                            if (wordDict.containsWord(gram3))
                                tokens.add(new SegToken(gram3, offset + j, offset + j + 3));
                        }
                    }
                    tokens.add(new SegToken(token, offset, offset += token.length()));
                }
            }

        return tokens;
    }

    /**
     * 处理单个句子的分词（保持原有代码结构）
     * 核心流程：
     * 1. 构建DAG → 2. 动态规划 → 3. 处理未登录词
     */
    public List<String> sentenceProcess(String sentence) {
        List<String> tokens = new ArrayList<String>();
        int N = sentence.length();
        Map<Integer, List<Integer>> dag = createDAG(sentence);
        Map<Integer, Pair<Integer>> route = calc(sentence, dag);

        int x = 0;
        int y = 0;
        String buf;
        StringBuilder sb = new StringBuilder();
        while (x < N) {
            y = route.get(x).key + 1;
            String lWord = sentence.substring(x, y);
            if (y - x == 1)
                sb.append(lWord);
            else {
                if (sb.length() > 0) {
                    buf = sb.toString();
                    sb = new StringBuilder();
                    if (buf.length() == 1) {
                        tokens.add(buf);
                    }
                    else {
                        if (wordDict.containsWord(buf)) {
                            tokens.add(buf);
                        }
                        else {
                            finalSeg.cut(buf, tokens);
                        }
                    }
                }
                tokens.add(lWord);
            }
            x = y;
        }
        buf = sb.toString();
        if (buf.length() > 0) {
            if (buf.length() == 1) {
                tokens.add(buf);
            }
            else {
                if (wordDict.containsWord(buf)) {
                    tokens.add(buf);
                }
                else {
                    finalSeg.cut(buf, tokens);
                }
            }

        }
        return tokens;
    }
}
