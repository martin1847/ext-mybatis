package com.bt.mybatis.runtime.bridge;

import java.io.IOException;
import java.io.Reader;

import org.apache.ibatis.builder.xml.XMLConfigBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;

public class XmlConfigurationFactory implements ConfigurationFactory {



    private String mybatisConfigFile;

    public XmlConfigurationFactory(){

    }

    public XmlConfigurationFactory(String mybatisConfigFile){
        this.mybatisConfigFile = mybatisConfigFile;
    }


    @Override
    public Configuration createConfiguration() {
        Reader reader = null;
        try {
            reader = Resources.getResourceAsReader(mybatisConfigFile);

            XMLConfigBuilder builder = new XMLConfigBuilder(reader);
            //just use the type full name
            //builder.getConfiguration().getTypeAliasRegistry().registerAlias("QUARKUS", QuarkusDataSourceFactory.class);
            return builder.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMybatisConfigFile() {
        return mybatisConfigFile;
    }

    public void setMybatisConfigFile(String mybatisConfigFile) {
        this.mybatisConfigFile = mybatisConfigFile;
    }

    @Override
    public String toString() {
        return "XmlConfigurationFactory{" +
                "mybatisConfigFile='" + mybatisConfigFile + '\'' +
                '}';
    }
}
