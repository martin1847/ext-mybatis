package tech.krpc.ext.it.mapper;

import java.util.List;
import java.util.Map;

import tech.krpc.mybatis.DbBounds;
import tech.krpc.ext.it.dto.Widget;

/**
 * The mapper under test. Registered as a CDI bean by the ext-mybatis deployment processor
 * (scanned from mybatis-config.xml's &lt;mappers&gt;&lt;package&gt;), injectable by type, and
 * registered for native reflection + JDK proxy. Its SQL lives in {@code mapper/WidgetMapper.xml}
 * (namespace = this interface's FQN), embedded in the native image as a resource by the
 * processor's NativeImageResourceBuildItem.
 */
public interface WidgetMapper {

    /** Plain query — exercises mapper proxy + DTO reflection + XML resource at native runtime. */
    List<Widget> listAll();

    /**
     * Paged query — exercises the PostgresPagingInterceptor: the count-rewrite (order-by/limit
     * stripped, wrapped in {@code select count(*)}) fills {@code bounds.count}, and offset/limit
     * are written back into the parameter map for the ANSI {@code limit/offset} SQL. The Map is
     * the effective parameter object (DbBounds is a MyBatis RowBounds, excluded from param
     * resolution); the interceptor mutates it in place.
     */
    List<Widget> listPaged(Map<String, Object> query, DbBounds bounds);
}
