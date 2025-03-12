package com.huaban.analysis.jieba;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 字典树节点实现（双数组Trie树优化版）
 * 功能：
 * 1. 存储词典中的词语
 * 2. 支持快速前缀匹配
 * 3. 动态扩展存储结构（数组+Map混合存储）
 * 
 * 实现特点：
 * - 小规模子节点使用数组存储（<=3个）
 * - 大规模子节点转为Map存储
 * - 支持动态添加和屏蔽词语
 */
class DictSegment implements Comparable<DictSegment> {
    // 全局字符缓存，减少重复对象创建
    private static final Map<Character, Character> charMap = new HashMap<>(16, 0.95f);
    
    // 数组存储的阈值，超过此值转为Map存储
    private static final int ARRAY_LENGTH_LIMIT = 3;

    // 子节点存储结构（根据storeSize动态切换）
    private Map<Character, DictSegment> childrenMap;  // Map存储结构
    private DictSegment[] childrenArray;              // 数组存储结构
    
    // 节点属性
    private final Character nodeChar;    // 当前节点代表的字符
    private int storeSize = 0;           // 子节点数量
    private int nodeState = 0;           // 节点状态 0-普通 1-词语结束

    /**
     * 节点构造函数
     * @param nodeChar 节点字符（不允许为空）
     */
    DictSegment(Character nodeChar) {
        if (nodeChar == null) {
            throw new IllegalArgumentException("节点字符不能为空");
        }
        this.nodeChar = nodeChar;
    }

    // 获取节点字符
    Character getNodeChar() {
        return nodeChar;
    }

    /**
     * 判断是否存在子节点
     * @return 存在子节点返回true
     */
    boolean hasNextNode() {
        return this.storeSize > 0;
    }

    /**
     * 词典匹配（正向最大匹配）
     * @param charArray 字符数组
     * @param begin 起始位置
     * @param length 匹配长度
     * @return 匹配结果（包含匹配状态和位置信息）
     */
    Hit match(char[] charArray, int begin, int length) {
        return this.match(charArray, begin, length, null);
    }

    // 核心匹配方法（递归实现）
    private Hit match(char[] charArray, int begin, int length, Hit searchHit) {
        if (searchHit == null) {
            searchHit = new Hit();
            searchHit.setBegin(begin);
        }
        searchHit.setEnd(begin);

        Character keyChar = charArray[begin];
        DictSegment ds = null;

        // 在子节点中查找匹配
        if (childrenArray != null) {
            // 数组二分查找
            DictSegment keySegment = new DictSegment(keyChar);
            int position = Arrays.binarySearch(childrenArray, 0, storeSize, keySegment);
            if (position >= 0) {
                ds = childrenArray[position];
            }
        } else if (childrenMap != null) {
            // Map直接查找
            ds = childrenMap.get(keyChar);
        }

        if (ds != null) {
            if (length > 1) {
                // 递归匹配后续字符
                return ds.match(charArray, begin + 1, length - 1, searchHit);
            } else if (length == 1) {
                // 到达词尾，设置匹配状态
                if (ds.nodeState == 1) {
                    searchHit.setMatch();
                }
                if (ds.hasNextNode()) {
                    searchHit.setPrefix();
                    searchHit.setMatchedDictSegment(ds);
                }
            }
        }
        return searchHit;
    }

    /**
     * 加载词语到字典树
     * @param charArray 词语字符数组
     */
    void fillSegment(char[] charArray) {
        this.fillSegment(charArray, 0, charArray.length, 1);
    }

    /**
     * 屏蔽字典中的词语
     * @param charArray 词语字符数组
     */
    void disableSegment(char[] charArray) {
        this.fillSegment(charArray, 0, charArray.length, 0);
    }

    // 核心加载方法（递归实现）
    private synchronized void fillSegment(char[] charArray, int begin, int length, int enabled) {
        // 字符规范化处理
        Character beginChar = charArray[begin];
        Character keyChar = charMap.computeIfAbsent(beginChar, k -> beginChar);

        // 查找或创建子节点
        DictSegment ds = lookforSegment(keyChar, enabled);
        if (ds != null) {
            if (length > 1) {
                ds.fillSegment(charArray, begin + 1, length - 1, enabled);
            } else if (length == 1) {
                ds.nodeState = enabled; // 设置词尾标记
            }
        }
    }

    // 查找/创建子节点
    private DictSegment lookforSegment(Character keyChar, int create) {
        if (storeSize <= ARRAY_LENGTH_LIMIT) {
            // 数组处理逻辑
            DictSegment[] segmentArray = getChildrenArray();
            int position = Arrays.binarySearch(segmentArray, 0, storeSize, new DictSegment(keyChar));
            
            if(position >= 0) return segmentArray[position];
            
            if(create == 1) {
                DictSegment newSegment = new DictSegment(keyChar);
                if(storeSize < ARRAY_LENGTH_LIMIT) {
                    segmentArray[storeSize] = newSegment;
                    storeSize++;
                    Arrays.sort(segmentArray, 0, storeSize);
                } else {
                    // 数组转Map
                    Map<Character, DictSegment> newMap = getChildrenMap();
                    migrate(segmentArray, newMap);
                    newMap.put(keyChar, newSegment);
                    storeSize++;
                    childrenArray = null; // 释放数组引用
                }
                return newSegment;
            }
        } else {
            // Map处理逻辑
            Map<Character, DictSegment> segmentMap = getChildrenMap();
            DictSegment ds = segmentMap.get(keyChar);
            if (ds == null && create == 1) {
                ds = new DictSegment(keyChar);
                segmentMap.put(keyChar, ds);
                storeSize++;
            }
            return ds;
        }
        return null;
    }

    // 线程安全的数组初始化
    private DictSegment[] getChildrenArray() {
        if (childrenArray == null) {
            synchronized (this) {
                if (childrenArray == null) {
                    childrenArray = new DictSegment[ARRAY_LENGTH_LIMIT];
                }
            }
        }
        return childrenArray;
    }

    // 线程安全的Map初始化
    private Map<Character, DictSegment> getChildrenMap() {
        if (childrenMap == null) {
            synchronized (this) {
                if (childrenMap == null) {
                    childrenMap = new HashMap<>(ARRAY_LENGTH_LIMIT * 2, 0.8f);
                }
            }
        }
        return childrenMap;
    }

    // 数据迁移（数组->Map）
    private void migrate(DictSegment[] segmentArray, Map<Character, DictSegment> segmentMap) {
        for (DictSegment seg : segmentArray) {
            if (seg != null) {
                segmentMap.put(seg.nodeChar, seg);
            }
        }
    }

    // 节点比较（用于数组排序）
    @Override
    public int compareTo(DictSegment o) {
        return this.nodeChar.compareTo(o.nodeChar);
    }
}