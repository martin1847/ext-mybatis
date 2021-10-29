package com.bt.mybatis.deployment;

import org.apache.ibatis.session.SqlSessionFactory;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.RuntimeValue;
import org.apache.ibatis.session.SqlSessionManager;

/**
 * Hold the RuntimeValue of {@link SqlSessionFactory}
 */
public final class SqlSessionFactoryBuildItem extends MultiBuildItem {
    private final RuntimeValue<SqlSessionFactory> sqlSessionFactory;


    private final RuntimeValue<SqlSessionManager> sqlSessionManager;


    private final String dataSourceName;
    private final boolean defaultDataSource;

    public SqlSessionFactoryBuildItem(RuntimeValue<SqlSessionFactory> sqlSessionFactory,
                                      RuntimeValue<SqlSessionManager> sqlSessionManager, String dataSourceName, boolean defaultDataSource) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.sqlSessionManager = sqlSessionManager;
        this.dataSourceName = dataSourceName;
        this.defaultDataSource = defaultDataSource;
    }

    public RuntimeValue<SqlSessionFactory> getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    public RuntimeValue<SqlSessionManager> getSqlSessionManager() {
        return sqlSessionManager;
    }
    public String getDataSourceName() {
        return dataSourceName;
    }

    public boolean isDefaultDataSource() {
        return defaultDataSource;
    }
}
