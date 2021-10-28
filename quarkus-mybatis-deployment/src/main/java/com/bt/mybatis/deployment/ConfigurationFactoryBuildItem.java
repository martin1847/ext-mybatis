package com.bt.mybatis.deployment;

import com.bt.mybatis.runtime.ConfigurationFactory;
import io.quarkus.builder.item.SimpleBuildItem;

public final class ConfigurationFactoryBuildItem extends SimpleBuildItem {
    private final ConfigurationFactory factory;

    public ConfigurationFactoryBuildItem(ConfigurationFactory factory) {
        this.factory = factory;
    }

    public ConfigurationFactory getFactory() {
        return factory;
    }
}
