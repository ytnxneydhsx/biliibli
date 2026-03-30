package com.bilibili.location.service.impl;

import com.bilibili.location.service.IpLocationResolver;
import com.bilibili.location.service.IpLocationService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisCachedIpLocationService implements IpLocationService {

    private static final String UNKNOWN_LOCATION = "未知";
    private static final String CACHE_KEY_PREFIX = "ip:location:";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final StringRedisTemplate stringRedisTemplate;
    private final IpLocationResolver ipLocationResolver;

    public RedisCachedIpLocationService(StringRedisTemplate stringRedisTemplate,
                                        IpLocationResolver ipLocationResolver) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.ipLocationResolver = ipLocationResolver;
    }

    @Override
    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN_LOCATION;
        }

        String normalizedIp = ip.trim();
        String cacheKey = CACHE_KEY_PREFIX + normalizedIp;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        String resolved = ipLocationResolver.resolveLocation(normalizedIp);
        String safeResolved = (resolved == null || resolved.isBlank()) ? UNKNOWN_LOCATION : resolved.trim();
        stringRedisTemplate.opsForValue().set(cacheKey, safeResolved, CACHE_TTL);
        return safeResolved;
    }
}
