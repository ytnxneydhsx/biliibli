package com.bilibili.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bilibili.config.StorageProperties;
import com.bilibili.mapper.VideoMapper;
import com.bilibili.mapper.VideoUploadTaskMapper;
import com.bilibili.model.dto.VideoUploadCompleteDTO;
import com.bilibili.model.dto.VideoUploadInitDTO;
import com.bilibili.model.entity.VideoDO;
import com.bilibili.model.entity.VideoUploadTaskDO;
import com.bilibili.model.vo.VideoUploadCompleteVO;
import com.bilibili.model.vo.VideoUploadInitVO;
import com.bilibili.model.vo.VideoUploadStatusVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VideoUploadServiceImplTest {

    @BeforeClass
    public static void initMybatisPlusLambdaCache() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, VideoUploadTaskDO.class);
        TableInfoHelper.initTableInfo(assistant, VideoDO.class);
    }

    @Mock
    private VideoUploadTaskMapper videoUploadTaskMapper;
    @Mock
    private VideoMapper videoMapper;
    @Mock
    private StorageProperties storageProperties;

    @InjectMocks
    private VideoUploadServiceImpl videoUploadService;

    private Path rootDir;

    @Before
    public void setUp() throws IOException {
        rootDir = Files.createTempDirectory("video-upload-test");
        when(storageProperties.getRootDir()).thenReturn(rootDir.toString());
        when(storageProperties.getPublicBaseUrl()).thenReturn("http://localhost:9000/media");
        when(storageProperties.getVideoSubDir()).thenReturn("video");
        when(storageProperties.getVideoMaxSize()).thenReturn(1024L * 1024 * 1024);
        when(storageProperties.getAllowedVideoTypes()).thenReturn("video/mp4");
    }

    @Test
    public void initUpload_shouldCreateTaskAndTempDir() {
        VideoUploadInitDTO dto = new VideoUploadInitDTO();
        dto.setFileName("demo.mp4");
        dto.setTotalSize(10L);
        dto.setChunkSize(4);
        dto.setTotalChunks(3);
        dto.setContentType("video/mp4");

        when(videoUploadTaskMapper.insert(any(VideoUploadTaskDO.class))).thenReturn(1);

        VideoUploadInitVO vo = videoUploadService.initUpload(1001L, dto);

        Assert.assertNotNull(vo.getUploadId());
        Assert.assertEquals(Integer.valueOf(4), vo.getChunkSize());
        Assert.assertEquals(Integer.valueOf(3), vo.getTotalChunks());

        ArgumentCaptor<VideoUploadTaskDO> captor = ArgumentCaptor.forClass(VideoUploadTaskDO.class);
        verify(videoUploadTaskMapper, times(1)).insert(captor.capture());
        VideoUploadTaskDO task = captor.getValue();
        Assert.assertEquals(Long.valueOf(1001L), task.getUserId());
        Assert.assertEquals("demo.mp4", task.getFileName());
        Assert.assertTrue(Files.exists(rootDir.resolve(task.getTempDir())));
    }

    @Test
    public void uploadChunk_shouldWriteChunkAndBeIdempotent() throws IOException {
        VideoUploadTaskDO task = buildTask("u1", "tmp/video/1001/u1", 8L, 5, 2, 0);
        when(videoUploadTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        MockMultipartFile chunk0 = new MockMultipartFile(
                "file", "0.part", "application/octet-stream", new byte[]{1, 2, 3, 4, 5});

        videoUploadService.uploadChunk(1001L, "u1", 0, chunk0);
        videoUploadService.uploadChunk(1001L, "u1", 0, chunk0);

        Path chunkPath = rootDir.resolve("tmp/video/1001/u1/0.part");
        Assert.assertTrue(Files.exists(chunkPath));
        Assert.assertEquals(5L, Files.size(chunkPath));
        verify(videoUploadTaskMapper, times(2)).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    public void getUploadStatus_shouldReturnUploadedChunkIndexesSorted() throws IOException {
        VideoUploadTaskDO task = buildTask("u2", "tmp/video/1001/u2", 20L, 5, 4, 0);
        when(videoUploadTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);

        Path dir = rootDir.resolve(task.getTempDir());
        Files.createDirectories(dir);
        Files.write(dir.resolve("3.part"), new byte[]{1});
        Files.write(dir.resolve("1.part"), new byte[]{1});
        Files.write(dir.resolve("0.part"), new byte[]{1});
        Files.write(dir.resolve("x.txt"), new byte[]{1});

        VideoUploadStatusVO vo = videoUploadService.getUploadStatus(1001L, "u2");

        Assert.assertEquals("u2", vo.getUploadId());
        Assert.assertEquals(Integer.valueOf(4), vo.getTotalChunks());
        Assert.assertEquals(Integer.valueOf(3), vo.getUploadedChunkCount());
        Assert.assertEquals(Arrays.asList(0, 1, 3), vo.getUploadedChunks());
        Assert.assertFalse(vo.getCompleted());
    }

    @Test
    public void completeUpload_shouldMergeChunksInsertVideoAndMarkDone() throws IOException {
        VideoUploadTaskDO task = buildTask("u3", "tmp/video/1001/u3", 5L, 3, 2, 0);
        when(videoUploadTaskMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(task);
        when(videoUploadTaskMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1, 1);
        when(videoMapper.insert(any(VideoDO.class))).thenAnswer(invocation -> {
            VideoDO video = invocation.getArgument(0);
            video.setId(88L);
            return 1;
        });

        Path dir = rootDir.resolve(task.getTempDir());
        Files.createDirectories(dir);
        Files.write(dir.resolve("0.part"), new byte[]{1, 2, 3});
        Files.write(dir.resolve("1.part"), new byte[]{4, 5});

        VideoUploadCompleteDTO dto = new VideoUploadCompleteDTO();
        dto.setTitle("demo title");
        dto.setDescription("demo desc");
        dto.setDuration(120L);

        VideoUploadCompleteVO vo = videoUploadService.completeUpload(1001L, "u3", dto);

        Assert.assertEquals("u3", vo.getUploadId());
        Assert.assertEquals(Long.valueOf(88L), vo.getVideoId());
        Assert.assertNotNull(vo.getVideoUrl());
        Assert.assertTrue(vo.getVideoUrl().startsWith("http://localhost:9000/media/video/"));

        String relativePath = vo.getVideoUrl().replace("http://localhost:9000/media/", "");
        Path mergedPath = rootDir.resolve(relativePath);
        Assert.assertTrue(Files.exists(mergedPath));
        Assert.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, Files.readAllBytes(mergedPath));
        Assert.assertFalse(Files.exists(dir));

        verify(videoUploadTaskMapper, times(2)).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(videoMapper, times(1)).insert(any(VideoDO.class));
    }

    private static VideoUploadTaskDO buildTask(String uploadId,
                                               String tempDir,
                                               long fileSize,
                                               int chunkSize,
                                               int totalChunks,
                                               int status) {
        VideoUploadTaskDO task = new VideoUploadTaskDO();
        task.setUploadId(uploadId);
        task.setUserId(1001L);
        task.setFileName("demo.mp4");
        task.setContentType("video/mp4");
        task.setFileSize(fileSize);
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setStatus(status);
        task.setTempDir(tempDir);
        task.setExpireTime(LocalDateTime.now().plusHours(1));
        return task;
    }
}

