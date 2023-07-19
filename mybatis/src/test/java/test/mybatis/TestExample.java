/**
 * Martin2038
 * Copyright (c) 2021-2022 All Rights Reserved.
 */
package test.mybatis;

import java.util.HashMap;

import com.bt.mybatis.DbBounds;
import org.junit.jupiter.api.Test;
import test.mybatis.example.ExampleMapper;

/**
 *
 * @author Martin.C
 * @version 2022/08/15 1:19 PM
 */
public class TestExample extends TestBase {

    @Test
    void  testPager(){
        try (var session = getSqlSessionFactory().openSession()) {
            var mapper = session.getMapper(ExampleMapper.class);
            var users = mapper.listBy(new HashMap<>(), DbBounds.fromPage(1,3));
            System.out.println(users);
        }
    }

}