//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package tech.krpc.mybatis.sqlsource;

import java.util.Map;
import java.util.function.Function;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.xmltags.DynamicContext;
import org.apache.ibatis.scripting.xmltags.DynamicSqlSource;
import org.apache.ibatis.scripting.xmltags.SqlNode;
import org.apache.ibatis.session.Configuration;

public class CountDynamicSqlSource extends CountSqlSource {
    Function<String, String> toCount;
    private Configuration configuration;
    private SqlNode rootSqlNode;

    public CountDynamicSqlSource(DynamicSqlSource sqlSource, Function<String, String> toCount) {
        MetaObject metaObject = SystemMetaObject.forObject(sqlSource);
        this.configuration = (Configuration)metaObject.getValue("configuration");
        this.rootSqlNode = (SqlNode)metaObject.getValue("rootSqlNode");
        this.toCount = toCount;
    }

    public BoundSql getBoundSql(Object parameterObject) {
        DynamicContext context = new DynamicContext(this.configuration, parameterObject);
        this.rootSqlNode.apply(context);
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(this.configuration);
        Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
        SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
        BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
        sqlSource = new StaticSqlSource(this.configuration, (String)this.toCount.apply(boundSql.getSql()), this.removeLimitOffset(boundSql.getParameterMappings()));
        boundSql = sqlSource.getBoundSql(parameterObject);

        for(Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
            boundSql.setAdditionalParameter((String)entry.getKey(), entry.getValue());
        }

        return boundSql;
    }
}