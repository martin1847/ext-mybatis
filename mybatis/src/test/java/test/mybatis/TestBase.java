/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2022 All Rights Reserved.
 */
package test.mybatis;

import java.io.IOException;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Martin.C
 * @version 2022/08/15 1:08 PM
 */
public abstract class TestBase {

    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }

    private    static SqlSessionFactory sqlSessionFactory;

    @BeforeAll
    static void initSql() throws IOException {
        String resource = "mybatis-config.xml";
        var inputStream = Resources.getResourceAsStream(resource);
        sqlSessionFactory =
                new SqlSessionFactoryBuilder().build(inputStream);
    }

    @AfterAll
    static void tearDownAll() {
    }



}