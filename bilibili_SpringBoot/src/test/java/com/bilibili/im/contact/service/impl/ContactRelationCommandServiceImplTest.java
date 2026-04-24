package com.bilibili.im.contact.service.impl;

import com.bilibili.im.contact.cache.ContactRelationSnapshotCache;
import com.bilibili.im.contact.mapper.ContactRelationMapper;
import com.bilibili.im.contact.model.cache.ContactRelationSnapshot;
import com.bilibili.im.metrics.ImDbOperationMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContactRelationCommandServiceImplTest {

    @Test
    void shouldSkipDbUpsertWhenDmContactAlreadyMarked() {
        ContactRelationMapper contactRelationMapper = mock(ContactRelationMapper.class);
        ContactRelationSnapshotCache contactRelationSnapshotCache = mock(ContactRelationSnapshotCache.class);
        ContactRelationCommandServiceImpl service = newService(contactRelationMapper, contactRelationSnapshotCache);

        when(contactRelationSnapshotCache.get(1001L, 1002L))
                .thenReturn(new ContactRelationSnapshot(1001L, 1002L, true, false, true, false, false));

        service.markDmContact(1001L, 1002L);

        verify(contactRelationMapper, never()).upsertDmContact(1001L, 1002L);
    }

    @Test
    void shouldUpsertDbWhenDmContactMissing() {
        ContactRelationMapper contactRelationMapper = mock(ContactRelationMapper.class);
        ContactRelationSnapshotCache contactRelationSnapshotCache = mock(ContactRelationSnapshotCache.class);
        ContactRelationCommandServiceImpl service = newService(contactRelationMapper, contactRelationSnapshotCache);

        when(contactRelationSnapshotCache.get(1001L, 1002L))
                .thenReturn(ContactRelationSnapshot.notFound(1001L, 1002L));
        when(contactRelationMapper.upsertDmContact(1001L, 1002L)).thenReturn(1);

        service.markDmContact(1001L, 1002L);

        verify(contactRelationMapper).upsertDmContact(1001L, 1002L);
    }

    private ContactRelationCommandServiceImpl newService(ContactRelationMapper contactRelationMapper,
                                                         ContactRelationSnapshotCache contactRelationSnapshotCache) {
        return new ContactRelationCommandServiceImpl(
                contactRelationMapper,
                contactRelationSnapshotCache,
                new ImDbOperationMetrics(new SimpleMeterRegistry())
        );
    }
}
