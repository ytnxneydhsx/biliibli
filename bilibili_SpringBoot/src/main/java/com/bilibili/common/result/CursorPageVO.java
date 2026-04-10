package com.bilibili.common.result;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CursorPageVO<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long nextCursor;

    private boolean hasMore;

    public static <T> CursorPageVO<T> of(List<T> records, Long nextCursor, boolean hasMore) {
        CursorPageVO<T> page = new CursorPageVO<>();
        page.setRecords(records);
        page.setNextCursor(nextCursor);
        page.setHasMore(hasMore);
        return page;
    }
}
