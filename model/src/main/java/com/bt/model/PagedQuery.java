/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.model;

import java.io.Serializable;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Martin.C
 * @version 2021/10/27 3:27 PM
 */
public class PagedQuery<Query> implements Serializable {



    public static final Integer DEFAULT_PAGE = 1;
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    Integer page;

    @NotNull@Min(1)@Max(100)
    Integer pageSize;

    //@NotNull
    @Valid
    Query q;

    public PagedQuery(){

    }

    public PagedQuery(Integer page, Integer pageSize) {
        this.page = page;
        this.pageSize = pageSize;
    }

    public PagedQuery(Integer page, Integer pageSize, Query q) {
        this.page = page;
        this.pageSize = pageSize;
        this.q = q;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Query getQ() {
        return q;
    }

    public void setQ(Query q) {
        this.q = q;
    }
}