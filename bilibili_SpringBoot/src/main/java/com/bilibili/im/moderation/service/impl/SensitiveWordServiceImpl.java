package com.bilibili.im.moderation.service.impl;

import com.bilibili.im.moderation.mapper.SensitiveWordMapper;
import com.bilibili.im.moderation.model.dto.CreateSensitiveWordDTO;
import com.bilibili.im.moderation.model.dto.UpdateSensitiveWordDTO;
import com.bilibili.im.moderation.model.entity.SensitiveWordDO;
import com.bilibili.im.moderation.model.vo.SensitiveWordVO;
import com.bilibili.im.moderation.service.SensitiveWordService;
import com.bilibili.tool.StringTool;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final SensitiveWordMapper sensitiveWordMapper;

    public SensitiveWordServiceImpl(SensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Override
    public Long createSensitiveWord(CreateSensitiveWordDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto is invalid");
        }

        String word = StringTool.normalizeRequired(dto.getWord(), "word");

        SensitiveWordDO sensitiveWord = new SensitiveWordDO();
        sensitiveWord.setWord(word);
        try {
            int rows = sensitiveWordMapper.insert(sensitiveWord);
            if (rows != 1 || sensitiveWord.getId() == null || sensitiveWord.getId() <= 0) {
                throw new RuntimeException("create sensitive word failed");
            }
            return sensitiveWord.getId();
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("sensitive word already exists");
        }
    }

    @Override
    public List<SensitiveWordVO> listSensitiveWords() {
        return mapToVOList(sensitiveWordMapper.selectAllOrderByIdAsc());
    }

    @Override
    public List<SensitiveWordVO> listActiveSensitiveWords() {
        return mapToVOList(sensitiveWordMapper.selectActiveOrderByIdAsc());
    }

    @Override
    public void updateSensitiveWord(Long id, UpdateSensitiveWordDTO dto) {
        validateSensitiveWordId(id);
        if (dto == null) {
            throw new IllegalArgumentException("dto is invalid");
        }

        String word = StringTool.normalizeRequired(dto.getWord(), "word");
        try {
            int rows = sensitiveWordMapper.updateWordById(id, word);
            if (rows != 1) {
                throw new IllegalArgumentException("sensitive word not found");
            }
        } catch (DuplicateKeyException ex) {
            throw new IllegalArgumentException("sensitive word already exists");
        }
    }

    @Override
    public void deleteSensitiveWord(Long id) {
        validateSensitiveWordId(id);
        int rows = sensitiveWordMapper.softDeleteById(id);
        if (rows != 1) {
            throw new IllegalArgumentException("sensitive word not found");
        }
    }

    private void validateSensitiveWordId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id is invalid");
        }
    }

    private List<SensitiveWordVO> mapToVOList(List<SensitiveWordDO> sensitiveWords) {
        if (sensitiveWords == null || sensitiveWords.isEmpty()) {
            return Collections.emptyList();
        }
        return sensitiveWords.stream()
                .map(this::toVO)
                .toList();
    }

    private SensitiveWordVO toVO(SensitiveWordDO sensitiveWord) {
        SensitiveWordVO vo = new SensitiveWordVO();
        vo.setId(sensitiveWord.getId());
        vo.setWord(sensitiveWord.getWord());
        vo.setStatus(sensitiveWord.getStatus());
        vo.setCreateTime(sensitiveWord.getCreateTime());
        vo.setUpdateTime(sensitiveWord.getUpdateTime());
        return vo;
    }
}
