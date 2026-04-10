package com.bilibili.im.moderation.service.impl;

import com.bilibili.im.moderation.mapper.SensitiveWordMapper;
import com.bilibili.im.moderation.model.dto.CreateSensitiveWordDTO;
import com.bilibili.im.moderation.model.dto.UpdateSensitiveWordDTO;
import com.bilibili.im.moderation.model.entity.SensitiveWordDO;
import com.bilibili.im.moderation.model.vo.SensitiveWordVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensitiveWordServiceImplTest {

    @Mock
    private SensitiveWordMapper sensitiveWordMapper;

    @InjectMocks
    private SensitiveWordServiceImpl sensitiveWordService;

    @Test
    void createSensitiveWordShouldTrimWordAndReturnId() {
        CreateSensitiveWordDTO dto = new CreateSensitiveWordDTO();
        dto.setWord("  test-word  ");

        when(sensitiveWordMapper.insert(any(SensitiveWordDO.class))).thenAnswer(invocation -> {
            SensitiveWordDO word = invocation.getArgument(0);
            word.setId(100L);
            return 1;
        });

        Long id = sensitiveWordService.createSensitiveWord(dto);

        assertEquals(100L, id);
        ArgumentCaptor<SensitiveWordDO> captor = ArgumentCaptor.forClass(SensitiveWordDO.class);
        verify(sensitiveWordMapper).insert(captor.capture());
        assertEquals("test-word", captor.getValue().getWord());
    }

    @Test
    void createSensitiveWordShouldRejectBlankWord() {
        CreateSensitiveWordDTO dto = new CreateSensitiveWordDTO();
        dto.setWord("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sensitiveWordService.createSensitiveWord(dto));

        assertEquals("word is required", ex.getMessage());
    }

    @Test
    void createSensitiveWordShouldRejectDuplicateWord() {
        CreateSensitiveWordDTO dto = new CreateSensitiveWordDTO();
        dto.setWord("dup-word");

        when(sensitiveWordMapper.insert(any(SensitiveWordDO.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sensitiveWordService.createSensitiveWord(dto));

        assertEquals("sensitive word already exists", ex.getMessage());
    }

    @Test
    void listSensitiveWordsShouldMapAllRecords() {
        SensitiveWordDO first = buildSensitiveWord(1L, "alpha", 0);
        SensitiveWordDO second = buildSensitiveWord(2L, "beta", 1);
        when(sensitiveWordMapper.selectAllOrderByIdAsc()).thenReturn(List.of(first, second));

        List<SensitiveWordVO> result = sensitiveWordService.listSensitiveWords();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("alpha", result.get(0).getWord());
        assertEquals(0, result.get(0).getStatus());
        assertEquals(2L, result.get(1).getId());
        assertEquals("beta", result.get(1).getWord());
        assertEquals(1, result.get(1).getStatus());
    }

    @Test
    void listActiveSensitiveWordsShouldMapActiveRecords() {
        SensitiveWordDO first = buildSensitiveWord(1L, "alpha", 0);
        SensitiveWordDO second = buildSensitiveWord(3L, "gamma", 0);
        when(sensitiveWordMapper.selectActiveOrderByIdAsc()).thenReturn(List.of(first, second));

        List<SensitiveWordVO> result = sensitiveWordService.listActiveSensitiveWords();

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("alpha", result.get(0).getWord());
        assertEquals(3L, result.get(1).getId());
        assertEquals("gamma", result.get(1).getWord());
    }

    @Test
    void updateSensitiveWordShouldTrimWord() {
        UpdateSensitiveWordDTO dto = new UpdateSensitiveWordDTO();
        dto.setWord("  new-word  ");
        when(sensitiveWordMapper.updateWordById(eq(5L), eq("new-word"))).thenReturn(1);

        sensitiveWordService.updateSensitiveWord(5L, dto);

        verify(sensitiveWordMapper).updateWordById(5L, "new-word");
    }

    @Test
    void updateSensitiveWordShouldRejectDuplicateWord() {
        UpdateSensitiveWordDTO dto = new UpdateSensitiveWordDTO();
        dto.setWord("dup-word");
        when(sensitiveWordMapper.updateWordById(eq(5L), eq("dup-word")))
                .thenThrow(new DuplicateKeyException("duplicate"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sensitiveWordService.updateSensitiveWord(5L, dto));

        assertEquals("sensitive word already exists", ex.getMessage());
    }

    @Test
    void updateSensitiveWordShouldRejectMissingRecord() {
        UpdateSensitiveWordDTO dto = new UpdateSensitiveWordDTO();
        dto.setWord("new-word");
        when(sensitiveWordMapper.updateWordById(eq(5L), eq("new-word"))).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sensitiveWordService.updateSensitiveWord(5L, dto));

        assertEquals("sensitive word not found", ex.getMessage());
    }

    @Test
    void deleteSensitiveWordShouldSoftDeleteById() {
        when(sensitiveWordMapper.softDeleteById(8L)).thenReturn(1);

        sensitiveWordService.deleteSensitiveWord(8L);

        verify(sensitiveWordMapper).softDeleteById(8L);
    }

    @Test
    void deleteSensitiveWordShouldRejectMissingRecord() {
        when(sensitiveWordMapper.softDeleteById(8L)).thenReturn(0);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> sensitiveWordService.deleteSensitiveWord(8L));

        assertEquals("sensitive word not found", ex.getMessage());
    }

    private SensitiveWordDO buildSensitiveWord(Long id, String word, Integer status) {
        SensitiveWordDO sensitiveWord = new SensitiveWordDO();
        sensitiveWord.setId(id);
        sensitiveWord.setWord(word);
        sensitiveWord.setStatus(status);
        sensitiveWord.setCreateTime(LocalDateTime.now());
        sensitiveWord.setUpdateTime(LocalDateTime.now());
        return sensitiveWord;
    }
}
