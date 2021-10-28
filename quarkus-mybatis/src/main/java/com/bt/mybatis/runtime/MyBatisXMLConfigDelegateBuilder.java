package com.bt.mybatis.runtime;

import java.io.Reader;

import com.bt.mybatis.runtime.config.MyBatisRuntimeConfig;
import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;

public class MyBatisXMLConfigDelegateBuilder implements XMLConfigDelegateBuilder {
    private XMLConfigBuilder     builder;
    private MyBatisRuntimeConfig config;

    public MyBatisXMLConfigDelegateBuilder() {

    }

    @Override
    public void setConfig(MyBatisRuntimeConfig config) {
        this.config = config;
    }

    @Override
    public Configuration getConfiguration() throws Exception {
        return getBuilder().getConfiguration();
    }

    @Override
    public Configuration parse() throws Exception {
        return getBuilder().parse();
    }

    private XMLConfigBuilder getBuilder() throws Exception {
        if (builder == null) {
            Reader reader = Resources.getResourceAsReader(config.xmlconfig.path);
            builder = new XMLConfigBuilder(reader, config.environment);
        }
        return builder;
    }
}
