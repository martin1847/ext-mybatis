

#  扩展自动扫描注入`Mapper`

基于[Quarkus](https://quarkus.io/)的自动扫描注入,以`quarkus-extension`扩展形式提供.

`globalRpcVersion`:`1.0.0`

```groovy
    implementation "tech.krpc.ext:ext-mybatis:$globalRpcVersion"
//    implementation "tech.krpc.mybatis:mybatis:$globalRpcVersion"
```

配置`mybatis-config.xml`

```xml
    <plugins>
        <plugin interceptor="tech.krpc.mybatis.MysqlPagingInterceptor"/>
    </plugins>


    <!-- setup environment with Quarkus data source -->
    <environments default="ds">
        <environment id="ds">
            <transactionManager type="MANAGED">
                <property name="closeConnection" value="false"/>
            </transactionManager>
            <dataSource type="tech.krpc.mybatis.runtime.bridge.QuarkusDataSourceFactory" />
<!--                <property name="db" value="h2"/>-->
<!--            </dataSource>-->
        </environment>
    </environments>

```

## 本地测试

```bash
gradle publishToMavenLocal
#mavenLocal()
```

## changelogs

* 2023-07-19 init1.0

