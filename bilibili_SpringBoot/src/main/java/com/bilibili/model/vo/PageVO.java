package com.bilibili.model.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

@Data
public class PageVO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> records;
    private Long total;
    private Long pageNo;
    private Long pageSize;
    private Long totalPages;

    public static <T> PageVO<T> from(IPage<T> page) {
        PageVO<T> vo = new PageVO<>();
        if (page == null) {
            vo.setRecords(Collections.emptyList());
            vo.setTotal(0L);
            vo.setPageNo(1L);
            vo.setPageSize(10L);
            vo.setTotalPages(0L);
            return vo;
        }
        vo.setRecords(page.getRecords() == null ? Collections.emptyList() : page.getRecords());
        vo.setTotal(page.getTotal());
        vo.setPageNo(page.getCurrent());
        vo.setPageSize(page.getSize());
        vo.setTotalPages(page.getPages());
        return vo;
    }
}
