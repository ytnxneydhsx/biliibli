package com.bilibili.im.common.time.impl;

import com.bilibili.im.common.time.ImTimeService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SystemImTimeService implements ImTimeService {

    @Override
    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
