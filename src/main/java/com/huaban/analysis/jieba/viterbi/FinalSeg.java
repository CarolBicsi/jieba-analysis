package com.huaban.analysis.jieba.viterbi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.Collections;

import com.huaban.analysis.jieba.CharacterUtil;
import com.huaban.analysis.jieba.Log;
import com.huaban.analysis.jieba.Pair;
import com.huaban.analysis.jieba.Node;

/**
 * 结巴分词HMM模型处理类
 * 功能：处理未登录词识别，通过隐马尔可夫模型（HMM）进行中文分词
 * 实现原理：
 * 1. 使用BMES状态标注体系：
 *    B-词首, M-词中, E-词尾, S-单字词
 * 2. 基于维特比算法求取最优状态路径
 * 3. 依赖预训练的概率模型（prob_emit.txt）
 */
public class FinalSeg {
    // 单例实例
    private static FinalSeg singleInstance;
    // 发射概率文件路径
    private static final String PROB_EMIT = "/prob_emit.txt";
    // 所有可能的状态
    private static char[] states = new char[] { 'B', 'M', 'E', 'S' };
    
    // 三大概率模型：
    private static Map<Character, Map<Character, Double>> emit;  // 发射概率（状态->字符->概率）
    private static Map<Character, Double> start;                 // 初始概率
    private static Map<Character, Map<Character, Double>> trans; // 转移概率
    
    // 状态转移约束（当前状态 -> 可能的前驱状态）
    private static Map<Character, char[]> prevStatus;
    // 最小概率值（用于log计算）
    private static Double MIN_FLOAT = -3.14e100;

    private FinalSeg() {
    }

    public synchronized static FinalSeg getInstance() {
        if (null == singleInstance) {
            singleInstance = new FinalSeg();
            singleInstance.loadModel();
        }
        return singleInstance;
    }

    /**
     * 加载HMM模型
     * 初始化内容：
     * 1. 状态转移规则
     * 2. 初始概率
     * 3. 转移概率矩阵
     * 4. 发射概率（从文件加载）
     */
    private void loadModel() {
        long s = System.currentTimeMillis();
        // 状态转移约束初始化
        prevStatus = new HashMap<Character, char[]>() {{
            put('B', new char[] {'E', 'S'}); // B前驱只能是E或S
            put('M', new char[] {'M', 'B'}); // M前驱只能是M或B
            put('S', new char[] {'S', 'E'}); // S前驱只能是S或E
            put('E', new char[] {'B', 'M'}); // E前驱只能是B或M
        }};
        
        // 初始概率（log值）
        start = new HashMap<Character, Double>() {{
            put('B', -0.26268660809250016);  // B的初始概率最高
            put('E', MIN_FLOAT);             // E不能作为开始状态
            put('M', MIN_FLOAT);             // M不能作为开始状态
            put('S', -1.4652633398537678);   // S的初始概率次之
        }};
        
        // 转移概率矩阵（log值）
        trans = new HashMap<Character, Map<Character, Double>>() {{
            put('B', new HashMap<Character, Double>() {{ put('E', -0.5108); put('M', -0.9163); }});
            put('E', new HashMap<Character, Double>() {{ put('B', -0.5897); put('S', -0.8085); }});
            put('M', new HashMap<Character, Double>() {{ put('E', -0.3334); put('M', -1.2604); }});
            put('S', new HashMap<Character, Double>() {{ put('B', -0.7212); put('S', -0.6659); }});
        }};
        
        // 加载发射概率文件
        InputStream is = this.getClass().getResourceAsStream(PROB_EMIT);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            emit = new HashMap<Character, Map<Character, Double>>();
            Map<Character, Double> values = null;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("\t");
                if (tokens.length == 1) {
                    values = new HashMap<Character, Double>();
                    emit.put(tokens[0].charAt(0), values);
                }
                else {
                    values.put(tokens[0].charAt(0), Double.valueOf(tokens[1]));
                }
            }
        }
        catch (IOException e) {
            Log.error(String.format(Locale.getDefault(), "%s: load model failure!", PROB_EMIT));
        }
        finally {
            try {
                if (null != is)
                    is.close();
            }
            catch (IOException e) {
                Log.error(String.format(Locale.getDefault(), "%s: close failure!", PROB_EMIT));
            }
        }
        Log.debug(String.format(Locale.getDefault(), "model load finished, time elapsed %d ms.",
            System.currentTimeMillis() - s));
    }

    /**
     * 主分词方法
     * @param sentence 待分词文本
     * @param tokens 分词结果容器
     * 处理流程：
     * 1. 分离中文字符和非中文字符
     * 2. 中文部分使用维特比算法处理
     * 3. 非中文部分按规则切分
     */
    public void cut(String sentence, List<String> tokens) {
        StringBuilder chinese = new StringBuilder();
        StringBuilder other = new StringBuilder();
        for (int i = 0; i < sentence.length(); ++i) {
            char ch = sentence.charAt(i);
            if (CharacterUtil.isChineseLetter(ch)) {
                if (other.length() > 0) {
                    processOtherUnknownWords(other.toString(), tokens);
                    other = new StringBuilder();
                }
                chinese.append(ch);
            }
            else {
                if (chinese.length() > 0) {
                    viterbi(chinese.toString(), tokens);
                    chinese = new StringBuilder();
                }
                other.append(ch);
            }
        }
        if (chinese.length() > 0)
            viterbi(chinese.toString(), tokens);
        else {
            processOtherUnknownWords(other.toString(), tokens);
        }
    }

    /**
     * 维特比算法实现
     * @param sentence 纯中文字符串
     * @param tokens 分词结果容器
     * 算法步骤：
     * 1. 初始化概率矩阵v和路径记录
     * 2. 前向传播计算最大概率路径
     * 3. 反向回溯获取最优状态序列
     * 4. 根据状态序列切分词语
     */
    public void viterbi(String sentence, List<String> tokens) {
        Vector<Map<Character, Double>> v = new Vector<>(); // 概率矩阵
        Map<Character, Node> path = new HashMap<>();       // 路径记录
        
        // 初始化第一个字符的概率
        v.add(new HashMap<>());
        for (char state : states) {
            Double emP = emit.get(state).getOrDefault(sentence.charAt(0), MIN_FLOAT);
            v.get(0).put(state, start.get(state) + emP);
            path.put(state, new Node(state, null));
        }
        
        // 前向传播计算概率
        for (int i = 1; i < sentence.length(); ++i) {
            Map<Character, Double> vv = new HashMap<Character, Double>();
            v.add(vv);
            Map<Character, Node> newPath = new HashMap<Character, Node>();
            for (char y : states) {
                Double emp = emit.get(y).get(sentence.charAt(i));
                if (emp == null)
                    emp = MIN_FLOAT;
                Pair<Character> candidate = null;
                for (char y0 : prevStatus.get(y)) {
                    Double tranp = trans.get(y0).get(y);
                    if (null == tranp)
                        tranp = MIN_FLOAT;
                    tranp += (emp + v.get(i - 1).get(y0));
                    if (null == candidate)
                        candidate = new Pair<Character>(y0, tranp);
                    else if (candidate.freq <= tranp) {
                        candidate.freq = tranp;
                        candidate.key = y0;
                    }
                }
                vv.put(y, candidate.freq);
                newPath.put(y, new Node(y, path.get(candidate.key)));
            }
            path = newPath;
        }
        
        // 回溯获取最优路径
        Vector<Character> posList = new Vector<>(sentence.length());
        double probE = v.get(sentence.length() - 1).get('E');
        double probS = v.get(sentence.length() - 1).get('S');
        Node win;
        if (probE < probS)
            win = path.get('S');
        else
            win = path.get('E');

        while (win != null) {
            posList.add(win.value);
            win = win.parent;
        }
        Collections.reverse(posList);
        
        // 根据状态序列切分词语
        int begin = 0, next = 0;
        for (int i = 0; i < sentence.length(); ++i) {
            char pos = posList.get(i);
            if (pos == 'B') begin = i;          // 记录词首位置
            else if (pos == 'E') {              // 遇到词尾，切分词语
                tokens.add(sentence.substring(begin, i + 1));
                next = i + 1;
            }
            else if (pos == 'S') {              // 单字词直接切分
                tokens.add(sentence.substring(i, i + 1));
                next = i + 1;
            }
        }
        // 处理剩余字符
        if (next < sentence.length()) {
            tokens.add(sentence.substring(next));
        }
    }

    private void processOtherUnknownWords(String other, List<String> tokens) {
        Matcher mat = CharacterUtil.reSkip.matcher(other);
        int offset = 0;
        while (mat.find()) {
            if (mat.start() > offset) {
                tokens.add(other.substring(offset, mat.start()));
            }
            tokens.add(mat.group());
            offset = mat.end();
        }
        if (offset < other.length())
            tokens.add(other.substring(offset));
    }
}
