package com.bilibili.im.moderation.service;

import com.bilibili.im.moderation.tool.SensitiveWordTrieNode;

public interface SensitiveWordTrieService {

    SensitiveWordTrieNode getCurrentTrie();

    SensitiveWordTrieNode refreshTrie();

    boolean containSensitiveWord(String text);
}
