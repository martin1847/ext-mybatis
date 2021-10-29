package com.bt.mybatis.runtime.bridge;

import org.apache.ibatis.session.Configuration;

public interface ConfigurationFactory {
    Configuration createConfiguration();
}
