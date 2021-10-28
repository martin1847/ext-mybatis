package com.bt.mybatis.runtime;

import org.apache.ibatis.session.Configuration;

public interface ConfigurationFactory {
    Configuration createConfiguration();
}
