package com.bt.mybatis.deployment;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;

import com.bt.mybatis.runtime.MyConfig;
import com.bt.mybatis.runtime.MyRecorder;
import com.bt.mybatis.runtime.bridge.QuarkusDataSource;
import com.bt.mybatis.runtime.bridge.QuarkusDataSourceFactory;
import com.bt.mybatis.runtime.bridge.XmlConfigurationFactory;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.ResultType;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;
import org.apache.ibatis.cache.decorators.LruCache;
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.javassist.util.proxy.ProxyFactory;
import org.apache.ibatis.logging.log4j.Log4jImpl;
import org.apache.ibatis.scripting.defaults.RawLanguageDriver;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.EnumTypeHandler;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

public class BtMybatisProcessor {

    private static final Logger LOG = Logger.getLogger(BtMybatisProcessor.class);
    private static final String FEATURE = "bt-mybatis";
    //private static final DotName MYBATIS_MAPPER = DotName.createSimple(Mapper.class.getName());
    //private static final DotName MYBATIS_TYPE_HANDLER = DotName.createSimple(MappedTypes.class.getName());
    //private static final DotName MYBATIS_JDBC_TYPE_HANDLER = DotName.createSimple(MappedJdbcTypes.class.getName());
    //private static final DotName MYBATIS_MAPPER_DATA_SOURCE = DotName.createSimple(MapperDataSource.class.getName());

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInitialzed(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInit) {
        runtimeInit.produce(new RuntimeInitializedClassBuildItem(Log4jImpl.class.getName()));
    }

    //@BuildStep
    //void reflectiveClasses(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
    //    reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
    //            ProxyFactory.class,
    //            XMLLanguageDriver.class,
    //            RawLanguageDriver.class,
    //            SelectProvider.class,
    //            UpdateProvider.class,
    //            InsertProvider.class,
    //            DeleteProvider.class,
    //            Result.class,
    //            Results.class,
    //            ResultType.class,
    //            ResultMap.class,
    //            EnumTypeHandler.class));
    //
    //    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
    //            PerpetualCache.class, LruCache.class));
    //}


    @BuildStep
    void addConfigurations(MyConfig config,
                           BuildProducer<ConfigurationMBI> configurations,
                           BuildProducer<MapperMBI> mappers,
                           BuildProducer<NativeImageResourceBuildItem> nativeResources,
                           BuildProducer<ReflectiveClassBuildItem> reflective,
                           BuildProducer<NativeImageProxyDefinitionBuildItem> proxy) throws Exception{

        var files = config.configFiles.split(",");
        var defaultAlias =  new Configuration().getTypeAliasRegistry().getTypeAliases();
        for(var configFile : files){


            //var cfgFac = new XmlConfigurationFactory(configFile);
            var cfg =  new XmlConfigurationFactory(configFile).createConfiguration();

            // TODO sql Mapfiles static init

            String xmlFolder = cfg.getVariables().getProperty("SqlXmlFolder");
            if(xmlFolder.charAt(0)=='/'){
                xmlFolder = xmlFolder.substring(1);
            }
            var folder = xmlFolder;

            URL resource =  Resources.getResourceURL(folder);

            LOG.info("found config file : " + configFile +" -> "+ folder +", url : "+resource);

            var sqlMaps =  Files.walk(Paths.get(resource.toURI()))
                    .filter(Files::isRegularFile)
                    .map(x ->   folder +"/" + x.getName(x.getNameCount()-1))
                    .filter(it->it.endsWith(".xml"))
                    .collect(Collectors.toList());



            //new NativeImageResourceBuildItem("META-INF/extra.properties");


            var ds =  (QuarkusDataSource)cfg.getEnvironment().getDataSource();

            var dsName = ds.getDataSourceName();

           for (var mapCls : cfg.getMapperRegistry().getMappers()){
               reflective.produce(new ReflectiveClassBuildItem(true, false, mapCls));
               proxy.produce(new NativeImageProxyDefinitionBuildItem(mapCls.getName()));

               LOG.info(" add Mapper Class for Reflective and Proxy :::  "+mapCls.getName());
               mappers.produce(new MapperMBI(DotName.createSimple(mapCls.getName()), dsName));
           }

           var alias = cfg.getTypeAliasRegistry().getTypeAliases();
           alias.forEach((k,clz)->{
               if(! defaultAlias.containsKey(k) && clz != QuarkusDataSourceFactory.class){
                   LOG.info("  Found Customer Alias  Types For Reflective :::  "+clz.getName());
                   reflective.produce(new ReflectiveClassBuildItem(true, true, clz));
               }
           });

            configurations.produce(new ConfigurationMBI(configFile,dsName,sqlMaps));
        }
    }

    @BuildStep
    NativeImageResourceBuildItem nativeImageResourceBuildItem(List<ConfigurationMBI> configurationMBIS) {
        List<String> resources = new ArrayList<>();
        configurationMBIS.forEach(it->resources.addAll(it.getMapperXml()));
        LOG.info("Reg nativeImageResource: "  + resources);
        return new NativeImageResourceBuildItem(resources);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void generateSqlSessionFactorys(List<ConfigurationMBI> configurationMBIS,
                                    BuildProducer<SqlSessionMBI> sqlSessionMBIBuildProducer,
                                    BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            MyRecorder recorder) throws  Exception {
        for(var cbi : configurationMBIS) {
            var factoryRuntime  = recorder.createSqlSessionFactory(
                    new XmlConfigurationFactory(cbi.getMybatisConfigFile()),cbi.getMapperXml());
            var sqlSessionMBI  = new SqlSessionMBI(factoryRuntime
                            , recorder.createSqlSessionManager(factoryRuntime)
                            , cbi.getDataSourceName(), cbi.isDefaultDs());
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(SqlSessionFactory.class)
                    .scope(Singleton.class)
                    .unremovable()
                    .supplier(recorder.MyBatisSqlSessionFactorySupplier(sqlSessionMBI.getSqlSessionFactory()));
            String dataSourceName = sqlSessionMBI.getDataSourceName();
            if (!sqlSessionMBI.isDefaultDataSource()) {
                configurator.defaultBean();
                configurator.addQualifier().annotation(Named.class).addValue("value", dataSourceName).done();
            }
            LOG.info("STATIC_INIT CDI SqlSessionFactory :"+ sqlSessionMBI);
            sqlSessionMBIBuildProducer.produce(sqlSessionMBI);
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
    }

    //
    //@Record(ExecutionTime.STATIC_INIT)
    //@BuildStep
    //void register(List<SqlSessionMBI> sqlSessionMBIS,
    //              BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
    //              MyRecorder recorder) {
    //    sqlSessionMBIS.forEach(sqlSessionMBI -> {
    //
    //    });
    //}


    //@Record(ExecutionTime.STATIC_INIT)
    //@BuildStep
    //void generateSqlSessionManager(List<SqlSessionFactoryBuildItem> sqlSessionFactoryBuildItems,
    //                               BuildProducer<SqlSessionManagerBuildItem> sqlSessionManager,
    //                               MyBatisRecorder recorder) {
    //    sqlSessionFactoryBuildItems.forEach(sessionFactory -> sqlSessionManager.produce(
    //            new SqlSessionManagerBuildItem(
    //                    recorder.createSqlSessionManager(sessionFactory.getSqlSessionFactory()),
    //                    sessionFactory.getDataSourceName(),
    //                    sessionFactory.isDefaultDataSource())));
    //}

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateMapperBeans(MyRecorder recorder,
                             List<MapperMBI> mapperMBIS,
                             //List<MyBatisMappedTypeBuildItem> myBatisMappedTypesBuildItems,
                             //List<MyBatisMappedJdbcTypeBuildItem> myBatisMappedJdbcTypesBuildItems,
                             List<SqlSessionMBI> sqlSessionFacItems,
                             BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        var dataSourceToSessionManagers = sqlSessionFacItems.stream()
                .collect(Collectors.toMap(SqlSessionMBI::getDataSourceName, SqlSessionMBI::getSqlSessionManager));
        for (MapperMBI i : mapperMBIS) {
            var sqlSessionManager = dataSourceToSessionManagers.get(i.getDataSourceName());
            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(i.getMapperName())
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    .supplier(recorder.MyBatisMapperSupplier(i.getMapperName().toString(),
                            sqlSessionManager));
            LOG.info("RUNTIME_INIT CDI Mapper Bean : " + i.getMapperName());
            syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        }
        //for (MyBatisMappedTypeBuildItem i : myBatisMappedTypesBuildItems) {
        //    SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
        //            .configure(i.getMappedTypeName())
        //            .scope(Singleton.class)
        //            .setRuntimeInit()
        //            .unremovable()
        //            .supplier(recorder.MyBatisMappedTypeSupplier(i.getMappedTypeName().toString(),
        //                    defaultSqlSessionManagerBuildItem.getSqlSessionManager()));
        //    syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        //}
        //for (MyBatisMappedJdbcTypeBuildItem i : myBatisMappedJdbcTypesBuildItems) {
        //    SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
        //            .configure(i.getMappedJdbcTypeName())
        //            .scope(Singleton.class)
        //            .setRuntimeInit()
        //            .unremovable()
        //            .supplier(recorder.MyBatisMappedJdbcTypeSupplier(i.getMappedJdbcTypeName().toString(),
        //                    defaultSqlSessionManagerBuildItem.getSqlSessionManager()));
        //    syntheticBeanBuildItemBuildProducer.produce(configurator.done());
        //}
    }



    //
    //
    //private SqlSessionManagerBuildItem getDefaultSessionManager(List<SqlSessionManagerBuildItem> sqlSessionManagerBuildItems) {
    //    return sqlSessionManagerBuildItems.stream()
    //            .filter(SqlSessionManagerBuildItem::isDefaultDataSource)
    //            .findFirst()
    //            .orElse(sqlSessionManagerBuildItems.get(0));
    //}


    //@Record(ExecutionTime.RUNTIME_INIT)
    //@BuildStep
    //void runInitialSql(List<SqlSessionFactoryBuildItem> sqlSessionFactoryBuildItems,
    //        MyBatisRuntimeConfig myBatisRuntimeConfig,
    //        MyBatisRecorder recorder) {
    //    sqlSessionFactoryBuildItems.forEach(sqlSessionFactoryBuildItem -> {
    //        MyBatisDataSourceRuntimeConfig dataSourceConfig = myBatisRuntimeConfig.dataSources
    //                .get(sqlSessionFactoryBuildItem.getDataSourceName());
    //        Optional<String> optionalInitialSql;
    //        if (sqlSessionFactoryBuildItem.isDefaultDataSource() || sqlSessionFactoryBuildItems.size() == 1) {
    //            optionalInitialSql = dataSourceConfig != null && dataSourceConfig.initialSql.isPresent()
    //                    ? dataSourceConfig.initialSql
    //                    : myBatisRuntimeConfig.initialSql;
    //        } else {
    //            optionalInitialSql = dataSourceConfig != null ? dataSourceConfig.initialSql : Optional.empty();
    //        }
    //        optionalInitialSql.ifPresent(initialSql -> recorder.runInitialSql(
    //                sqlSessionFactoryBuildItem.getSqlSessionFactory(), initialSql));
    //    });
    //}

    //
    //@BuildStep
    //@Overridable
    //void addMyBatisMappers(BuildProducer<MyBatisMapperBuildItem> mappers,
    //        BuildProducer<ReflectiveClassBuildItem> reflective,
    //        BuildProducer<NativeImageProxyDefinitionBuildItem> proxy,
    //        CombinedIndexBuildItem indexBuildItem) {
    //    for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(MYBATIS_MAPPER)) {
    //        if (i.target().kind() == AnnotationTarget.Kind.CLASS) {
    //            DotName dotName = i.target().asClass().name();
    //            System.out.println("::: 116 Find Mapper AnntoinaClass : " + dotName);
    //            reflective.produce(new ReflectiveClassBuildItem(true, false, dotName.toString()));
    //            proxy.produce(new NativeImageProxyDefinitionBuildItem(dotName.toString()));
    //
    //            Optional<AnnotationInstance> mapperDatasource = i.target().asClass().annotations().entrySet().stream()
    //                    .filter(entry -> entry.getKey().equals(MYBATIS_MAPPER_DATA_SOURCE))
    //                    .map(Map.Entry::getValue)
    //                    .map(annotationList -> annotationList.get(0))
    //                    .findFirst();
    //            if (mapperDatasource.isPresent()) {
    //                String dataSourceName = mapperDatasource.get().value().asString();
    //                mappers.produce(new MyBatisMapperBuildItem(dotName, dataSourceName));
    //            } else {
    //                mappers.produce(new MyBatisMapperBuildItem(dotName, "<default>"));
    //            }
    //        }
    //    }
    //}

    //@BuildStep
    //void addMyBatisMappedTypes(MyBatisRuntimeConfig config,BuildProducer<MyBatisMappedTypeBuildItem> mappedTypes,
    //        BuildProducer<MyBatisMappedJdbcTypeBuildItem> mappedJdbcTypes,
    //        CombinedIndexBuildItem indexBuildItem) {
    //
    //
    //    List<DotName> names = new ArrayList<>();
    //    for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(MYBATIS_TYPE_HANDLER)) {
    //        if (i.target().kind() == AnnotationTarget.Kind.CLASS) {
    //            DotName dotName = i.target().asClass().name();
    //            mappedTypes.produce(new MyBatisMappedTypeBuildItem(dotName));
    //            names.add(dotName);
    //        }
    //    }
    //    for (AnnotationInstance i : indexBuildItem.getIndex().getAnnotations(MYBATIS_JDBC_TYPE_HANDLER)) {
    //        if (i.target().kind() == AnnotationTarget.Kind.CLASS) {
    //            DotName dotName = i.target().asClass().name();
    //            if (!names.contains(dotName)) {
    //                mappedJdbcTypes.produce(new MyBatisMappedJdbcTypeBuildItem(dotName));
    //            }
    //        }
    //    }
    //}

    //@BuildStep
    //void initialSql(BuildProducer<NativeImageResourceBuildItem> resource, MyBatisRuntimeConfig config) {
    //    config.initialSql.ifPresent(initialSql -> resource.produce(new NativeImageResourceBuildItem(initialSql)));
    //    config.dataSources.values().forEach(dataSource -> dataSource.initialSql
    //            .ifPresent(initialSql -> resource.produce(new NativeImageResourceBuildItem(initialSql))));
    //}

    //@Record(ExecutionTime.STATIC_INIT)
    //@BuildStep
    //void generateSqlSessionFactory(MyBatisRuntimeConfig config,MyBatisRuntimeConfig myBatisRuntimeConfig,
    //        ConfigurationBuildItem configurationFactoryBuildItem,
    //        SqlSessionFactoryBuilderBuildItem sqlSessionFactoryBuilderBuildItem,
    //        List<MyBatisMapperBuildItem> myBatisMapperBuildItems,
    //        List<MyBatisMappedTypeBuildItem> myBatisMappedTypeBuildItems,
    //        List<MyBatisMappedJdbcTypeBuildItem> myBatisMappedJdbcTypeBuildItems,
    //        List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem,
    //        BuildProducer<SqlSessionFactoryBuildItem> sqlSessionFactory,
    //        MyBatisRecorder recorder) {
    //
    //
    //
    //    List<String> mappedTypes = myBatisMappedTypeBuildItems
    //            .stream().map(m -> m.getMappedTypeName().toString()).collect(Collectors.toList());
    //    List<String> mappedJdbcTypes = myBatisMappedJdbcTypeBuildItems
    //            .stream().map(m -> m.getMappedJdbcTypeName().toString()).collect(Collectors.toList());
    //
    //    List<Pair<String, Boolean>> dataSources = new ArrayList<>();
    //    if (myBatisRuntimeConfig.dataSource.isPresent()) {
    //        String dataSourceName = myBatisRuntimeConfig.dataSource.get();
    //        Optional<JdbcDataSourceBuildItem> jdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
    //                .filter(i -> i.getName().equals(dataSourceName))
    //                .findFirst();
    //        if (!jdbcDataSourceBuildItem.isPresent()) {
    //            throw new ConfigurationError("Can not find datasource " + dataSourceName);
    //        }
    //        dataSources.add(Pair.of(dataSourceName, true));
    //    } else {
    //        dataSources = jdbcDataSourcesBuildItem.stream()
    //                .map(dataSource -> Pair.of(dataSource.getName(), dataSource.isDefault()))
    //                .collect(Collectors.toList());
    //        if (dataSources.isEmpty()) {
    //            throw new ConfigurationError("No datasource found");
    //        }
    //    }
    //
    //    dataSources.forEach(dataSource -> {
    //        MyBatisDataSourceRuntimeConfig dataSourceConfig = myBatisRuntimeConfig.dataSources.get(dataSource.getKey());
    //        List<String> mappers = myBatisMapperBuildItems
    //                .stream().filter(m -> m.getDataSourceName().equals(dataSource.getKey()))
    //                .map(m -> m.getMapperName().toString()).collect(Collectors.toList());
    //        sqlSessionFactory.produce(
    //                new SqlSessionFactoryBuildItem(
    //                        recorder.createSqlSessionFactory(
    //                                configurationFactoryBuildItem.getFactory(),
    //                                sqlSessionFactoryBuilderBuildItem.getBuilder(),
    //                                myBatisRuntimeConfig,
    //                                dataSourceConfig,
    //                                dataSource.getKey(),
    //                                mappers,
    //                                mappedTypes,
    //                                mappedJdbcTypes),
    //                        dataSource.getKey(), dataSource.getValue(), false));
    //    });
    //}


    //
    //@BuildStep
    //@Overridable
    //SqlSessionFactoryBuilderBuildItem createSqlSessionFactoryBuilder() {
    //    return new SqlSessionFactoryBuilderBuildItem(new SqlSessionFactoryBuilder());
    //}


    //@BuildStep
    //void xmlConfig(MyBatisRuntimeConfig config, BuildProducer<MyBatisXmlConfigBuildItem> xmlConfig) {
    //    if (config.xmlconfig.enable == true) {
    //        xmlConfig.produce(new MyBatisXmlConfigBuildItem("xmlconfig", true));
    //    }
    //}


}
