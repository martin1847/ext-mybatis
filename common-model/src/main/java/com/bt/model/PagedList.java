package com.bt.model;


import java.io.Serializable;
import java.util.List;

public class PagedList<DTO> implements Serializable {

    /// int is enough.maybeNull when use lastkey
    Integer count;

    /// current page data
    List<DTO> data;

    /// use last key to paging
    String lastKey;

    public PagedList() {
    }

    public PagedList(Integer count, List<DTO> data) {
        this.count = count;
        this.data = data;
    }



    public PagedList( String lastKey, List<DTO> data) {
        this.lastKey = lastKey;
        this.data = data;
    }

    /**
     * Getter method for property <tt>count</tt>.
     *
     * @return property value of count
     */
    public Integer getCount() {
        return count;
    }

    /**
     * Setter method for property <tt>count</tt>.
     *
     * @param count  value to be assigned to property count
     */
    public void setCount(Integer count) {
        this.count = count;
    }

    /**
     * Getter method for property <tt>data</tt>.
     *
     * @return property value of data
     */
    public List<DTO> getData() {
        return data;
    }

    /**
     * Setter method for property <tt>data</tt>.
     *
     * @param data  value to be assigned to property data
     */
    public void setData(List<DTO> data) {
        this.data = data;
    }

    /**
     * Getter method for property <tt>lastKey</tt>.
     *
     * @return property value of lastKey
     */
    public String getLastKey() {
        return lastKey;
    }

    /**
     * Setter method for property <tt>lastKey</tt>.
     *
     * @param lastKey  value to be assigned to property lastKey
     */
    public void setLastKey(String lastKey) {
        this.lastKey = lastKey;
    }

    @Override
    public String toString() {
        return "PagedList{" +
                "count=" + count +
                ", data=" + data +
                ", lastKey='" + lastKey + '\'' +
                '}';
    }
}