package tech.krpc.mybatis.it;

import org.apache.ibatis.annotations.Param;

public interface WidgetMapper {

    /** Non-transactional read — the historical leak path under MANAGED + closeConnection=false. */
    Widget getWidget(Integer id);

    Integer save(@Param("id") Integer id, @Param("name") String name);

    boolean remove(Integer id);
}
