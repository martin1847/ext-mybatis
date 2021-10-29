package com.bt.mybatis.deployment;

import com.bt.mybatis.runtime.bridge.QuarkusDataSourceFactory;
import io.quarkus.builder.item.MultiBuildItem;

// need to be serialize
public final class ConfigurationMBI extends MultiBuildItem  {



    private String mybatisConfigFile;


    private String dataSourceName;

    //private  Configuration cfg;

    public ConfigurationMBI(){

    }

    public ConfigurationMBI(String mybatisConfigFile){
        this(mybatisConfigFile,null);
    }

    public ConfigurationMBI(String mybatisConfigFile, String dataSourceName) {

        this.mybatisConfigFile = mybatisConfigFile;
        this.dataSourceName = dataSourceName;



    }
    //
    //public Configuration buildTimeConfiguration() {
    //    return  builder.getConfiguration();
    //}




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
