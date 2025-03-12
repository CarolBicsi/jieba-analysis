package com.huaban.analysis.jieba;

import java.io.BufferedReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * 结巴分词核心字典类
 * 实现功能：
 * 1. 词典管理：加载主词典（dict.txt）和用户自定义词典
 * 2. 字典树构建：使用Trie树结构存储词典
 * 3. 词频管理：提供词语频率查询和归一化处理
 * 
 * 核心设计：
 * - 单例模式：保证全局唯一字典实例
 * - 延迟加载：首次使用时加载主词典
 * - 扩展性：支持动态加载用户词典
 */
public class WordDictionary {
    // 单例实例（双重校验锁实现）
    private static WordDictionary singleton;
    // 主词典路径（内置于JAR包中）
    private static final String MAIN_DICT = "/dict.txt";
    // 用户词典后缀
    private static String USER_DICT_SUFFIX = ".dict";

    // 词频表（词语 -> 对数概率值）
    public final Map<String, Double> freqs = new HashMap<String, Double>();
    // 已加载词典路径记录（避免重复加载）
    public final Set<String> loadedPath = new HashSet<String>();
    // 最小词频值（用于未登录词）
    private Double minFreq = Double.MAX_VALUE;
    // 总词频数（用于归一化）
    private Double total = 0.0;
    // 字典树根节点
    private DictSegment _dict;

    private WordDictionary() {
        this.loadDict();
    }

    public static WordDictionary getInstance() {
        if (singleton == null) {
            synchronized (WordDictionary.class) {
                if (singleton == null) {
                    singleton = new WordDictionary();
                    return singleton;
                }
            }
        }
        return singleton;
    }

    /**
     * for ES to initialize the user dictionary.
     * 
     * @param configFile
     */
    public void init(Path configFile) {
        String abspath = configFile.toAbsolutePath().toString();
        Log.debug("initialize user dictionary:" + abspath);
        synchronized (WordDictionary.class) {
            if (loadedPath.contains(abspath)) {
                return;
            }
            
            DirectoryStream<Path> stream;
            try {
                stream = Files.newDirectoryStream(configFile, String.format(Locale.getDefault(), "*%s", USER_DICT_SUFFIX));
                for (Path path: stream){
                    Log.error(String.format(Locale.getDefault(), "loading dict %s", path.toString()));
                    singleton.loadUserDict(String.valueOf(path));
                }
                loadedPath.add(abspath);
            } catch (IOException e) {
                Log.error(String.format(Locale.getDefault(), "%s: load user dict failure!", configFile.toString()));
            }
        }
    }
    
    public void init(String[] paths) {
        synchronized (WordDictionary.class) {
            for (String path: paths){
                if (!loadedPath.contains(path)) {
                    try {
                        Log.debug("initialize user dictionary: " + path);
                        singleton.loadUserDict(path);
                        loadedPath.add(path);
                    } catch (Exception e) {
                        Log.error(String.format(Locale.getDefault(), "%s: load user dict failure!", path));
                    }
                }
            }
        }
    }
    
    /**
     * let user just use their own dict instead of the default dict
     */
    public void resetDict(){
    	_dict = new DictSegment((char) 0);
    	freqs.clear();
    }

    /**
     * 初始化主词典
     * 处理流程：
     * 1. 创建字典树根节点
     * 2. 读取内置词典文件（UTF-8编码）
     * 3. 构建字典树并计算词频
     * 4. 归一化词频（取自然对数）
     */
    public void loadDict() {
        _dict = new DictSegment((char) 0);
        InputStream is = this.getClass().getResourceAsStream(MAIN_DICT);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            long s = System.currentTimeMillis();
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 2) {
                    continue;
                }

                String word = tokens[0];
                double freq = Double.valueOf(tokens[1]);
                total += freq;
                word = addWord(word);
                freqs.put(word, freq);
            }
            // normalize
            for (Entry<String, Double> entry : freqs.entrySet()) {
                entry.setValue((Math.log(entry.getValue() / total)));
                minFreq = Math.min(entry.getValue(), minFreq);
            }
            Log.debug(String.format(Locale.getDefault(), "main dict load finished, time elapsed %d ms",
                    System.currentTimeMillis() - s));
        }
        catch (IOException e) {
            Log.error(String.format(Locale.getDefault(), "%s load failure!", MAIN_DICT));
        }
        finally {
            try {
                if (null != is) {
                    is.close();
                }
            }
            catch (IOException e) {
                Log.error(String.format(Locale.getDefault(), "%s close failure!", MAIN_DICT));
            }
        }
    }

    private String addWord(String word) {
        if (null != word && !"".equals(word.trim())) {
            String key = word.trim().toLowerCase(Locale.getDefault());
            _dict.fillSegment(key.toCharArray());
            return key;
        }
        else {
            return null;
        }
    }

    /**
     * 加载用户自定义词典
     * @param userDict 用户词典路径
     * @param charset 文件编码格式
     * 特点：
     * - 支持多词典文件加载（.dict后缀）
     * - 默认词频3.0（当用户未指定时）
     * - 自动合并到主字典树
     */
    public void loadUserDict(Path userDict, Charset charset) {                
        try {
            BufferedReader br = Files.newBufferedReader(userDict, charset);
            long s = System.currentTimeMillis();
            int count = 0;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 1) {
                    // Ignore empty line
                    continue;
                }

                String word = tokens[0];

                double freq = 3.0d;
                if (tokens.length == 2) {
                    freq = Double.valueOf(tokens[1]);
                }
                word = addWord(word); 
                freqs.put(word, Math.log(freq / total));
                count++;
            }
            Log.debug(String.format(Locale.getDefault(), "user dict %s load finished, tot words:%d, time elapsed:%dms", userDict.toString(), count, System.currentTimeMillis() - s));
            br.close();
        }
        catch (IOException e) {
            Log.error(String.format(Locale.getDefault(), "%s: load user dict failure!", userDict.toString()));
        }
    }

    public void loadUserDict(String userDictPath) {
        loadUserDict(userDictPath, StandardCharsets.UTF_8);
    }
    
    public void loadUserDict(String userDictPath, Charset charset) {
        InputStream is = this.getClass().getResourceAsStream(userDictPath);
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, charset));

            long s = System.currentTimeMillis();
            int count = 0;
            while (br.ready()) {
                String line = br.readLine();
                String[] tokens = line.split("[\t ]+");

                if (tokens.length < 1) {
                    // Ignore empty line
                    continue;
                }

                String word = tokens[0];

                double freq = 3.0d;
                if (tokens.length == 2) {
                    freq = Double.valueOf(tokens[1]);
                }
                word = addWord(word);
                freqs.put(word, Math.log(freq / total));
                count++;
            }
            Log.debug(String.format(Locale.getDefault(), "user dict %s load finished, tot words:%d, time elapsed:%dms", userDictPath, count, System.currentTimeMillis() - s));
            br.close();
        }
        catch (IOException e) {
            Log.error(String.format(Locale.getDefault(), "%s: load user dict failure!", userDictPath));
        }
    }
    
    /**
     * 字典树访问接口
     * @return 字典树根节点
     * 用途：供JiebaSegmenter分词时使用
     */
    public DictSegment getTrie() {
        return this._dict;
    }

    public boolean containsWord(String word) {
        return freqs.containsKey(word);
    }

    /**
     * 词频查询方法
     * @param key 词语
     * @return 对数概率值
     * 策略：未登录词返回最小词频值
     */
    public Double getFreq(String key) {
        if (containsWord(key)) {
            return freqs.get(key);
        } else {
            return minFreq;
        }
    }
}
