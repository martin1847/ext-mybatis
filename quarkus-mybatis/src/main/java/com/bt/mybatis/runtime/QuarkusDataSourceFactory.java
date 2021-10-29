package com.bt.mybatis.runtime;

import java.util.Properties;

import javax.sql.DataSource;

import org.apache.ibatis.datasource.DataSourceFactory;

public class QuarkusDataSourceFactory implements DataSourceFactory {

    public static final String DEFAULT_DS_NAME = "<default>";


    private Properties properties;
    private QuarkusDataSource dataSource;

    public QuarkusDataSourceFactory() {
    }

    @Override
    public void setProperties(Properties properties) {
        this.properties = properties;
        if (dataSource == null) {
            dataSource = new QuarkusDataSource(properties.getProperty("db", DEFAULT_DS_NAME));
        }
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
}
