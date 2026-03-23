package com.bilibili.video.hot;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bilibili.config.properties.VideoHotProperties;
import com.bilibili.video.mapper.VideoMapper;
import com.bilibili.video.model.vo.VideoVO;
import com.bilibili.video.redis.repository.VideoHotRedisRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VideoHotBootstrapService implements ApplicationRunner {

    private final VideoMapper videoMapper;
    private final VideoHotProperties videoHotProperties;
    private final VideoHotRedisRepository videoHotRedisRepository;

    public VideoHotBootstrapService(VideoMapper videoMapper,
                                    VideoHotProperties videoHotProperties,
                                    VideoHotRedisRepository videoHotRedisRepository) {
        this.videoMapper = videoMapper;
        this.videoHotProperties = videoHotProperties;
        this.videoHotRedisRepository = videoHotRedisRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Page<VideoVO> page = new Page<>(1, videoHotProperties.getRankSize());
        List<VideoVO> videos = videoMapper.selectPublishedVideosByViewCount(page).getRecords();
        videoHotRedisRepository.bootstrapActiveSlot(videos);
    }
}
