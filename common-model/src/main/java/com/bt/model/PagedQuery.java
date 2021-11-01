/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 *
 * @author Martin.C
 * @version 2021/10/27 3:27 PM
 */
public class PagedQuery<Query> {

    public static final Integer DEFAULT_PAGE = 1;
    public static final Integer DEFAULT_PAGE_SIZE = 10;

    Integer page;

    @NotNull@Min(1)
    Integer pageSize;

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

    /**
     * Getter method for property <tt>page</tt>.
     *
     * @return property value of page
     */
    public Integer getPage() {
        return page;
    }

    /**
     * Setter method for property <tt>page</tt>.
     *
     * @param page  value to be assigned to property page
     */
    public void setPage(Integer page) {
        this.page = page;
    }

    /**
     * Getter method for property <tt>pageSize</tt>.
     *
     * @return property value of pageSize
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * Setter method for property <tt>pageSize</tt>.
     *
     * @param pageSize  value to be assigned to property pageSize
     */
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Getter method for property <tt>q</tt>.
     *
     * @return property value of q
     */
    public Query getQ() {
        return q;
    }

    /**
     * Setter method for property <tt>q</tt>.
     *
     * @param q  value to be assigned to property q
     */
    public void setQ(Query q) {
        this.q = q;
    }
}