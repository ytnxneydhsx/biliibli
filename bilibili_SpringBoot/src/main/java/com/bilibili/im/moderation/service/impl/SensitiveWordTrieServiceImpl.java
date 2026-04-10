package com.bilibili.im.moderation.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bilibili.im.moderation.mapper.SensitiveWordMapper;
import com.bilibili.im.moderation.model.entity.SensitiveWordDO;
import com.bilibili.im.moderation.service.SensitiveWordTrieService;
import com.bilibili.im.moderation.tool.SensitiveWordTextCleaner;
import com.bilibili.im.moderation.tool.SensitiveWordTrieNode;

import jakarta.annotation.PostConstruct;

@Service
public class SensitiveWordTrieServiceImpl implements SensitiveWordTrieService {

    private final SensitiveWordMapper sensitiveWordMapper;

    private volatile SensitiveWordTrieNode currentTrie = new SensitiveWordTrieNode();

    public SensitiveWordTrieServiceImpl(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @PostConstruct
    public void initializeTrie() {
        refreshTrie();
    }

    @Override
    public SensitiveWordTrieNode getCurrentTrie() {
        return currentTrie;
    }

    @Override
    public boolean containSensitiveWord(String text){
        String normalizedText = SensitiveWordTextCleaner.normalizeForMatch(text);
        if (normalizedText == null || normalizedText.isBlank()) {
            return false;
        }

        SensitiveWordTrieNode root = currentTrie;
        for (int i = 0; i < normalizedText.length(); i++) {
            SensitiveWordTrieNode currentNode = root;
            for (int j = i; j < normalizedText.length(); j++) {
                char c = normalizedText.charAt(j);
                currentNode = currentNode.getChildren().get(c);
                if (currentNode == null) {
                    break;
                }
                if (currentNode.isEnd()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public SensitiveWordTrieNode refreshTrie() {
        SensitiveWordTrieNode newTrie = new SensitiveWordTrieNode();
        List<SensitiveWordDO> sensitiveWords = sensitiveWordMapper.selectActiveOrderByIdAsc();
        if (sensitiveWords != null) {
            for (SensitiveWordDO sensitiveWord : sensitiveWords) {
                if (sensitiveWord == null) {
                    continue;
                }
                String word = sensitiveWord.getWord();
                if (word == null || word.isBlank()) {
                    continue;
                }
                newTrie.insert(word);
            }
        }
        currentTrie = newTrie;
        return newTrie;
    }
}
