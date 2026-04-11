package com.hotel.common;

import lombok.Data;

@Data
public class PageResult<T> {

    private Long total;

    private T records;

    public PageResult(Long total, T records) {
        this.total = total;
        this.records = records;
    }
}
