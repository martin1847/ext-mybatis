package com.bt.mybatis.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class MapperMBI extends MultiBuildItem {
    private final DotName mapperName;
    private final String dataSourceName;

    public MapperMBI(DotName mapperName, String dataSourceName) {
        this.mapperName = mapperName;
        this.dataSourceName = dataSourceName;
    }

    public DotName getMapperName() {
        return mapperName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    @Override
    public String toString() {
        return "{" +  mapperName + "->" + dataSourceName + '}';
    }
}
