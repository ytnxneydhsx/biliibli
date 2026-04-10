package com.bilibili.im.moderation.service;

import com.bilibili.im.moderation.model.dto.CreateSensitiveWordDTO;
import com.bilibili.im.moderation.model.dto.UpdateSensitiveWordDTO;
import com.bilibili.im.moderation.model.vo.SensitiveWordVO;

import java.util.List;

public interface SensitiveWordService {

    Long createSensitiveWord(CreateSensitiveWordDTO dto);

    List<SensitiveWordVO> listSensitiveWords();

    List<SensitiveWordVO> listActiveSensitiveWords();

    void updateSensitiveWord(Long id, UpdateSensitiveWordDTO dto);

    void deleteSensitiveWord(Long id);
}
