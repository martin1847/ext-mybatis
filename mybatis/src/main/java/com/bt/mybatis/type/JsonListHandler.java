package com.bt.mybatis.type;

import java.util.List;


/**
 * date: 16/8/15 16:14
 *
 * @author: yangyang.cyy@alibaba-inc.com
 */
public abstract class JsonListHandler<T> extends JsonTypeHandler<List<T>> {

    //protected final Class<T> tClass;
    //
    //{
    //    tClass = (Class<T>) getParameterizedTypes(this)[0];
    //}

    @Override
    protected abstract List<T> parseJSON(String json);
    //{
    //    return JsonUtils.parseArray(json, tClass);
    //}
}
