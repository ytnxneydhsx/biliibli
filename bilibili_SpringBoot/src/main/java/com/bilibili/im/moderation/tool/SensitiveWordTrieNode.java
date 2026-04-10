package com.bilibili.im.moderation.tool;

import java.util.HashMap;
import java.util.Map;

public class SensitiveWordTrieNode {


    private boolean end;


    private final Map<Character, SensitiveWordTrieNode> children = new HashMap<>();

    public boolean isEnd() {
        return end;
    }

    public void setEnd(boolean end) {
        this.end = end;
    }

    public Map<Character, SensitiveWordTrieNode> getChildren() {
        return children;
    }

    public void addChild(Character c, SensitiveWordTrieNode node) {
        children.put(c, node);
    }

    public void insert(String word) {
        SensitiveWordTrieNode currentNode = this;
        for (char c : word.toCharArray()) {
            currentNode = currentNode.children.computeIfAbsent(c, k -> new SensitiveWordTrieNode());
        }
        currentNode.setEnd(true);
    }

    public boolean Iscontains(String word) {
        SensitiveWordTrieNode currentNode = this;
        for (char c : word.toCharArray()) {
            currentNode = currentNode.children.get(c);
            if (currentNode == null) {
                return false;
            }
        }
        return currentNode.isEnd();
    }

}
