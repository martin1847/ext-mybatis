package tech.krpc.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * End-to-end integration test for {@link PostgresPagingInterceptor} against a real
 * PostgreSQL 17 instance.
 *
 * <p>The sibling {@link PostgresPagingInterceptorTest} only asserts the {@code getCountSql}
 * string rewrite in isolation. This test drives the full framework paging path:
 * {@link DbBounds} (RowBounds) -> interceptor count query -> data query using ANSI
 * {@code limit #{limit} offset #{offset}}, and verifies page content, page size, offset,
 * the count returned by the rewritten count statement, and order-by stripping.
 *
 * <p>Self-contained and repeatable: it builds its own {@link Configuration} programmatically
 * (registering {@link PostgresPagingInterceptor} as a plugin) instead of the MySQL-oriented
 * mybatis-config.xml, creates {@code t_paging_it} with N known rows at setup, and drops it
 * at teardown.
 *
 * <p>If the database is unreachable (e.g. {@code build -x test} / CI without the container),
 * an {@link Assumptions} guard skips the test rather than failing the build.
 */
class PostgresPagingIntegrationTest {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5433/krpc_test";
    private static final String JDBC_USER = "krpc";
    private static final String JDBC_PASSWORD = "krpc";

    private static final String TABLE = "t_paging_it";
    private static final int ROW_COUNT = 25;

    private static SqlSessionFactory sqlSessionFactory;
    private static boolean dbAvailable;

    /** Inline mapper exercising the framework paging path with PG ANSI limit/offset. */
    public interface PagingMapper {
        /** No order by — relies on table insert order. */
        List<Row> listAll(@Param("dummy") Object ignored, DbBounds bounds);

        /** With order by — count rewrite must strip the trailing order by. */
        List<Row> listOrdered(@Param("dummy") Object ignored, DbBounds bounds);
    }

    public static class Row {
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @BeforeAll
    static void setUp() throws SQLException {
        dbAvailable = canConnect();
        Assumptions.assumeTrue(dbAvailable,
                "PostgreSQL not reachable at " + JDBC_URL + " — skipping real-DB paging integration test");

        DataSource dataSource = new UnpooledDataSource(
                "org.postgresql.Driver", JDBC_URL, JDBC_USER, JDBC_PASSWORD);

        // Fresh, deterministic fixture: drop + create + seed N known rows.
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("drop table if exists " + TABLE);
            st.execute("create table " + TABLE + " (id integer primary key, name varchar(64) not null)");
            StringBuilder insert = new StringBuilder("insert into " + TABLE + " (id, name) values ");
            for (int i = 1; i <= ROW_COUNT; i++) {
                if (i > 1) {
                    insert.append(", ");
                }
                insert.append("(").append(i).append(", 'name-").append(i).append("')");
            }
            st.execute(insert.toString());
        }

        Configuration configuration = new Configuration(
                new Environment("it", new JdbcTransactionFactory(), dataSource));
        configuration.setMapUnderscoreToCamelCase(true);
        // The interceptor under test.
        configuration.addInterceptor(new PostgresPagingInterceptor());
        configuration.getTypeAliasRegistry().registerAlias("row", Row.class);

        String namespace = PagingMapper.class.getName();
        // listAll: no order by
        addSelect(configuration, namespace + ".listAll",
                "select id, name from " + TABLE + " limit #{limit} offset #{offset}");
        // listOrdered: with order by — exercises count rewrite stripping order by
        addSelect(configuration, namespace + ".listOrdered",
                "select id, name from " + TABLE + " order by id desc limit #{limit} offset #{offset}");
        configuration.addMapper(PagingMapper.class);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void addSelect(Configuration configuration, String id, String sql) {
        // Wrap with <script> so #{} placeholders are parsed as a dynamic SqlSource,
        // mirroring how XML <select> bodies are handled at runtime.
        String script = "<script>" + sql + "</script>";
        org.apache.ibatis.mapping.SqlSource sqlSource =
                configuration.getLanguageRegistry().getDefaultDriver()
                        .createSqlSource(configuration, script, Map.class);
        org.apache.ibatis.mapping.MappedStatement ms =
                new org.apache.ibatis.mapping.MappedStatement.Builder(
                        configuration, id, sqlSource,
                        org.apache.ibatis.mapping.SqlCommandType.SELECT)
                        .resultMaps(List.of(
                                new org.apache.ibatis.mapping.ResultMap.Builder(
                                        configuration, id + "-rm", Row.class, List.of()).build()))
                        .build();
        configuration.addMappedStatement(ms);
    }

    @AfterAll
    static void tearDown() throws SQLException {
        if (!dbAvailable) {
            return;
        }
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement st = conn.createStatement()) {
            st.execute("drop table if exists " + TABLE);
        }
    }

    private static boolean canConnect() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    @Test
    void paginatesFirstPage_withoutOrderBy() {
        // page 1, size 10 => offset 0, limit 10, needCount true
        DbBounds bounds = DbBounds.fromPage(1, 10);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PagingMapper mapper = session.getMapper(PagingMapper.class);
            List<Row> rows = mapper.listAll(new HashMap<>(), bounds);

            assertEquals(10, rows.size(), "page size honored");
            assertEquals(ROW_COUNT, bounds.getCount(), "count query returns total N");
            assertEquals(1, rows.get(0).getId(), "first row of page 1 is id=1");
            assertEquals(10, rows.get(9).getId(), "last row of page 1 is id=10");
        }
    }

    @Test
    void paginatesMiddlePage_offsetApplied() {
        // page 3, size 10 => offset 20, limit 10 => rows 21..25 (only 5 left)
        DbBounds bounds = DbBounds.fromPage(3, 10);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PagingMapper mapper = session.getMapper(PagingMapper.class);
            List<Row> rows = mapper.listAll(new HashMap<>(), bounds);

            assertEquals(5, rows.size(), "last page has remainder rows");
            assertEquals(ROW_COUNT, bounds.getCount(), "count is total N regardless of page");
            assertEquals(21, rows.get(0).getId(), "offset=20 -> first row id=21");
            assertEquals(25, rows.get(4).getId(), "last row id=25");
        }
    }

    @Test
    void paginatesWithOrderBy_countStripsOrderBy() {
        // order by id desc; page 1 size 7 => ids 25..19
        DbBounds bounds = DbBounds.fromPage(1, 7);
        try (SqlSession session = sqlSessionFactory.openSession()) {
            PagingMapper mapper = session.getMapper(PagingMapper.class);
            List<Row> rows = mapper.listOrdered(new HashMap<>(), bounds);

            assertEquals(7, rows.size(), "page size honored with order by");
            assertEquals(ROW_COUNT, bounds.getCount(),
                    "count query returns N even though source query had order by/limit");
            assertEquals(25, rows.get(0).getId(), "order by id desc -> first row id=25");
            assertEquals(19, rows.get(6).getId(), "7th row id=19");
            for (int i = 1; i < rows.size(); i++) {
                assertTrue(rows.get(i - 1).getId() > rows.get(i).getId(), "descending order preserved");
            }
        }
    }
}
