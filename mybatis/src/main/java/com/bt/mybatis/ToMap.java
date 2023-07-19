/**
 * Martin2038
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.mybatis;

import java.util.Map;

/**
 *
 * @author Martin.C
 * @version 2021/11/01 1:25 PM
 */
@FunctionalInterface
public interface ToMap<DTO> {

    Map<String,Object> to(DTO dto);
}