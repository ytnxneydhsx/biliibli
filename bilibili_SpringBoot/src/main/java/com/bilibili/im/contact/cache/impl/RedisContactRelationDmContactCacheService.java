package com.bilibili.im.contact.cache.impl;

import com.bilibili.im.contact.cache.ContactRelationCacheKeys;
import com.bilibili.im.contact.cache.ContactRelationCacheTuning;
import com.bilibili.im.contact.cache.ContactRelationDmContactCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisContactRelationDmContactCacheService implements ContactRelationDmContactCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisContactRelationDmContactCacheService.class);
    private static final String DM_CONTACT_VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisContactRelationDmContactCacheService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean hasDmContact(Long ownerUserId, Long targetUserId) {
        try {
            String key = ContactRelationCacheKeys.dmContactKey(ownerUserId, targetUserId);
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn("read dm contact cache failed, fallback to db, ownerUserId={}, targetUserId={}",
                    ownerUserId, targetUserId, ex);
            return false;
        }
    }

    @Override
    public void markDmContact(Long ownerUserId, Long targetUserId) {
        try {
            String key = ContactRelationCacheKeys.dmContactKey(ownerUserId, targetUserId);
            stringRedisTemplate.opsForValue().set(
                    key,
                    DM_CONTACT_VALUE,
                    ContactRelationCacheTuning.DM_CONTACT_TTL
            );
        } catch (RedisConnectionFailureException | RedisSystemException ex) {
            log.warn("write dm contact cache failed, ownerUserId={}, targetUserId={}",
                    ownerUserId, targetUserId, ex);
        }
    }
}
