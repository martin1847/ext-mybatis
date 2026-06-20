package tech.krpc.mybatis;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * PostgreSQL 物理分页拦截器（PG 方言，覆盖至 PG 17）。
 *
 * <p>{@link AbstractPagingInterceptor} 把 offset/limit 作为参数（OFFSET/LIMIT key）
 * 写回 parameterObject，分页 SQL 由 mapper 用 ANSI {@code limit #{limit} offset #{offset}}
 * 拼接 —— 本类只需提供 count 查询的 SQL 改写（剥 order by / limit 后套 count(*)）。
 * count 改写为纯 ANSI，与 {@link MysqlPagingInterceptor} 同逻辑，PG 直接复用。
 *
 * <p>启用方式：在 mybatis-config.xml 的 &lt;plugins&gt; 中注册本类替代
 * MysqlPagingInterceptor：
 * <pre>{@code
 * <plugins>
 *     <plugin interceptor="tech.krpc.mybatis.PostgresPagingInterceptor"/>
 * </plugins>
 * }</pre>
 * 框架本身不做方言开关，按数据库选择注册哪一个拦截器即可。
 */
@Intercepts(
        @Signature(type = Executor.class, method = "query",
                args = {MappedStatement.class, Object.class,
                        RowBounds.class, ResultHandler.class})
)
public class PostgresPagingInterceptor extends AbstractPagingInterceptor {

    public static final String ORDER_BY = "order by";

    public static final String UNION = "union";
    public static final String LIMIT = "limit";
    public static final String FROM  = "from";

    @Override
    protected String getCountSql(String targetSql) {
        String sql = targetSql.toLowerCase();
        StringBuilder sqlBuilder = new StringBuilder(sql);

        int trimPos = 0;
        if ((trimPos = sqlBuilder.lastIndexOf(ORDER_BY)) != -1 || (trimPos = sqlBuilder.lastIndexOf(LIMIT)) != -1) {
            sqlBuilder.delete(trimPos, sqlBuilder.length());
        }

        if (sqlBuilder.indexOf(UNION) != -1) {
            sqlBuilder.insert(0, "select count(*) from ( ").append(" ) tmp_inner_union_alias ");
            return sqlBuilder.toString();
        }

        int fromPos = sqlBuilder.indexOf(FROM);
        if (fromPos != -1) {
            sqlBuilder.delete(0, fromPos);
            sqlBuilder.insert(0, "select count(*) ");
        }

        return sqlBuilder.toString();
    }
}
