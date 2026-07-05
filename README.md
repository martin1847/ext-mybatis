

#  扩展自动扫描注入`Mapper`

基于[Quarkus](https://quarkus.io/)的自动扫描注入,以`quarkus-extension`扩展形式提供.

`globalRpcVersion`:`1.0.0`

```groovy
    implementation "tech.krpc.ext:ext-mybatis:$globalRpcVersion"
//    implementation "tech.krpc.mybatis:mybatis:$globalRpcVersion"
```

配置 `mybatis-config.xml`（最小配置，弱事务、吞吐优先——KRPC 生态默认取向）：

```xml
    <plugins>
        <plugin interceptor="tech.krpc.mybatis.MysqlPagingInterceptor"/>
    </plugins>

    <!-- setup environment with Quarkus data source -->
    <environments default="ds">
        <environment id="ds">
            <transactionManager type="MANAGED"/>
            <dataSource type="tech.krpc.mybatis.runtime.bridge.QuarkusDataSourceFactory"/>
        </environment>
    </environments>
```

> 连接生命周期由框架保证：非事务操作（每次 mapper 调用即一次 auto-commit）用完即把连接归还
> Agroal 池——你**无需**配置 `closeConnection`。ext-mybatis 在 `QuarkusDataSource` 拓扑下强制
> 连接归还语义（见 `XmlConfigurationFactory`），所以出厂最小配置默认不漏连接。
>
> ⚠️ 历史提示：早期文档示例写过 `<property name="closeConnection" value="false"/>`。在
> Quarkus + Agroal 下**没有**"容器收尾"钩子来归还非事务连接，该配置会导致每次非事务读泄漏一条
> 连接直至池耗尽。现在框架会覆盖它为安全语义并**每个 classloader warn 一次**(Quarkus 在
> augmentation 与 runtime 两个 classloader 各加载一次本类,故各出一条,非全局一条);请从配置里删除该属性。

### 显式事务（重场景 opt-in）

多写不变量等重场景用 JTA 显式事务：在 service 方法上加 `@Transactional`（需要
`quarkus-narayana-jta`）。事务方法内的连接生命周期由**事务**管理——连接在事务提交/回滚时
才归还池，事务内多条 mapper 调用复用同一条 enlisted 连接，`MANAGED` 的每调用 close 只释放
Agroal wrapper、不会提前结束 JTA 事务（提交原子性与回滚均成立）。

```java
@ApplicationScoped
public class OrderService {
    @Inject OrderMapper mapper;

    @Transactional // 多写在同一事务里，全部成功或全部回滚
    public void placeOrder(Order o) {
        mapper.insertOrder(o);
        mapper.decrementStock(o.getItemId(), o.getQty());
    }
}
```

## 本地测试

```bash
gradle publishToMavenLocal
#mavenLocal()
```

## changelogs

* 2023-07-19 init1.0
* 2026-07-05 conn-leak fix (EXTMYB-LEAK-001): 生态默认取向为弱事务、吞吐优先——非事务操作用完
  即还连接，重场景用 `@Transactional`/JTA 显式 opt-in（事务结束才还）。为此，`MANAGED` +
  `QuarkusDataSource` 拓扑下框架强制连接归还语义（覆盖消费者显式的 `closeConnection=false` 并
  warn 一次/classloader），修复非事务读每请求泄漏一条连接、打满池即 acquisition timeout 的缺陷。**行为变更**：
  之前显式设 `closeConnection=false` 的配置，其非事务读连接现在会被归还（此前是泄漏）；事务语义不变。

