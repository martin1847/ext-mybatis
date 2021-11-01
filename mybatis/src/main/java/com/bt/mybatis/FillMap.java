/**
 * Botaoyx.com Inc.
 * Copyright (c) 2021-2021 All Rights Reserved.
 */
package com.bt.mybatis;

import java.util.Map;

/**
 *
 * @author Martin.C
 * @version 2021/11/01 1:39 PM
 */
@FunctionalInterface
public interface FillMap<DTO> {
    void fill(DTO dto, Map<String,Object> map);
}