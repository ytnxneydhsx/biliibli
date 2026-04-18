package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.cache.ContactRelationDmContactCacheService;
import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.metrics.ImDbOperationMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactRelationCommandServiceImplTest {

    @Test
    void shouldSkipDbUpsertWhenDmContactCacheHit() {
        ContactRelationMapper contactRelationMapper = mock(ContactRelationMapper.class);
        ContactRelationDmContactCacheService dmContactCacheService = mock(ContactRelationDmContactCacheService.class);
        ContactRelationCommandServiceImpl service = newService(contactRelationMapper, dmContactCacheService);

        when(dmContactCacheService.hasDmContact(1001L, 1002L)).thenReturn(true);

        service.markDmContact(1001L, 1002L);

        verify(contactRelationMapper, never()).upsertDmContact(1001L, 1002L);
        verify(dmContactCacheService, never()).markDmContact(1001L, 1002L);
    }

    @Test
    void shouldUpsertDbAndMarkCacheOnCacheMiss() {
        ContactRelationMapper contactRelationMapper = mock(ContactRelationMapper.class);
        ContactRelationDmContactCacheService dmContactCacheService = mock(ContactRelationDmContactCacheService.class);
        ContactRelationCommandServiceImpl service = newService(contactRelationMapper, dmContactCacheService);

        when(dmContactCacheService.hasDmContact(1001L, 1002L)).thenReturn(false);
        when(contactRelationMapper.upsertDmContact(1001L, 1002L)).thenReturn(1);

        service.markDmContact(1001L, 1002L);

        verify(contactRelationMapper).upsertDmContact(1001L, 1002L);
        verify(dmContactCacheService).markDmContact(1001L, 1002L);
    }

    private ContactRelationCommandServiceImpl newService(ContactRelationMapper contactRelationMapper,
                                                        ContactRelationDmContactCacheService dmContactCacheService) {
        return new ContactRelationCommandServiceImpl(
                contactRelationMapper,
                dmContactCacheService,
                new ImDbOperationMetrics(new SimpleMeterRegistry())
        );
    }
}
