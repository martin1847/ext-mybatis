package com.bt.mybatis.deployment;

import java.util.List;

import com.bt.mybatis.runtime.bridge.QuarkusDataSourceFactory;
import io.quarkus.builder.item.MultiBuildItem;

// need to be serialize
public final class ConfigurationMBI extends MultiBuildItem  {



    private String mybatisConfigFile;


    private String dataSourceName;

    private List<String> mapperXml;


    public ConfigurationMBI(){

    }

    public ConfigurationMBI(String mybatisConfigFile){
        this(mybatisConfigFile,null,null);
    }

    public ConfigurationMBI(String mybatisConfigFile, String dataSourceName, List<String> mapperXml) {

        this.mybatisConfigFile = mybatisConfigFile;
        this.dataSourceName = dataSourceName;
        this.mapperXml = mapperXml;
    }
    //
    //public Configuration buildTimeConfiguration() {
    //    return  builder.getConfiguration();
    //}



    public List<String> getMapperXml() {
        return mapperXml;
    }
    public void setMapperXml(List<String> mapperXml) {
        this.mapperXml = mapperXml;
    }


    public boolean isDefaultDs(){
        return QuarkusDataSourceFactory.DEFAULT_DS_NAME.equals(dataSourceName);
    }

    public String getMybatisConfigFile() {
        return mybatisConfigFile;
    }

    public void setMybatisConfigFile(String mybatisConfigFile) {
        this.mybatisConfigFile = mybatisConfigFile;
    }
    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }
}
