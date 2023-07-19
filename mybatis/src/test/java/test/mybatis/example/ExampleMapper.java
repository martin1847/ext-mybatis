/**
 * Martin2038
 * Copyright (c) 2021-2022 All Rights Reserved.
 */
package test.mybatis.example;

import java.util.List;
import java.util.Map;

import com.bt.mybatis.DbBounds;
import test.mybatis.dto.User;

/**
 *
 * @author Martin.C
 * @version 2022/08/15 1:16 PM
 */
public interface ExampleMapper {

    List<User> listBy(Map<String,Object> query, DbBounds bounds);
}