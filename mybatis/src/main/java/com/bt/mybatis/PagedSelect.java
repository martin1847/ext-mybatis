/**
 * Martin2038
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.mybatis;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Martin.C
 * @version 2021/11/01 1:45 PM
 */
@FunctionalInterface
public interface PagedSelect<DTO> {
    List<DTO> list(Map<String,Object> query, DbBounds bounds);
}