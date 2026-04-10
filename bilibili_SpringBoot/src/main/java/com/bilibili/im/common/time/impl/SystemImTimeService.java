package com.bilibili.im.common.time.impl;

import com.bilibili.im.common.time.ImTimeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class SystemImTimeService implements ImTimeService {

    private static final ZoneId ZONE_BEIJING = ZoneId.of("Asia/Shanghai");

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now(ZONE_BEIJING);
    }
}
