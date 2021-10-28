package com.bt.mybatis.runtime;

import com.bt.mybatis.runtime.config.MyBatisRuntimeConfig;
import org.apache.ibatis.session.Configuration;

public interface XMLConfigDelegateBuilder {
    void setConfig(MyBatisRuntimeConfig config);

    Configuration getConfiguration() throws Exception;

    Configuration parse() throws Exception;
}
