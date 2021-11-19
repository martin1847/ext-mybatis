/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.mybatis.type;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import com.bt.rpc.util.JsonUtils;
import com.bt.rpc.util.ParameterizedTypeImpl;

/**
 *
 * @author Martin.C
 * @version 2021/11/19 7:28 PM
 */
public abstract class AbstractJsonListHandler<T> extends JsonTypeHandler<List<T>> {

    protected final ParameterizedType listOf;

    {
        var tClass = (Class<T>) getParameterizedTypes(this)[0];
        listOf = new  ParameterizedTypeImpl(List.class, tClass);
    }

    @Override
    protected String stringify(List<T> obj) {
        if(obj == null){
            return null;
        }
        return JsonUtils.stringify(obj);
    }

    @Override
    protected List<T> parseJSON(String json){
        if(json == null || json.isBlank()){
            return null;
        }
        return JsonUtils.parse(json, listOf);
    }
}