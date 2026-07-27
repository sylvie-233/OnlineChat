package com.sylvie233.common.model.resp;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

/**
 * 分页响应体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PageResult<T> extends Result<List<T>> {

    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(List<T> data, long total, long page, long size) {
        PageResult<T> r = new PageResult<>();
        r.setCode(200);
        r.setMessage("ok");
        r.setData(data);
        r.setTotal(total);
        r.setPage(page);
        r.setSize(size);
        return r;
    }
}
