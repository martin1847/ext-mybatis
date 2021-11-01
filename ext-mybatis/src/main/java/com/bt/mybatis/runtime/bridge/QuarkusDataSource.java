package com.bt.mybatis.runtime.bridge;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.DataSources;

import javax.sql.DataSource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;

public class QuarkusDataSource implements DataSource {
    private String           dataSourceName;
    private AgroalDataSource dataSource;

    public QuarkusDataSource(String dataSourceName) {
        this.dataSourceName = dataSourceName;
        this.dataSource = null;
    }

    private DataSource getDataSource() {
        if (dataSource == null) {
            dataSource = DataSources.fromName(dataSourceName);
        }
        return dataSource;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    @Override
    public Connection getConnection(String user, String passwd) throws SQLException {
        return getDataSource().getConnection(user, passwd);
    }

    @Override
    public <T> T unwrap(Class<T> aClass) throws SQLException {
        return getDataSource().unwrap(aClass);
    }

    @Override
    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        return getDataSource().isWrapperFor(aClass);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return getDataSource().getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter printWriter) throws SQLException {
        getDataSource().setLogWriter(printWriter);
    }

    @Override
    public void setLoginTimeout(int timeout) throws SQLException {
        getDataSource().setLoginTimeout(timeout);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return getDataSource().getLoginTimeout();
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return getDataSource().getParentLogger();
    }
}