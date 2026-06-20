package tech.krpc.mybatis;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pure string-rewrite unit tests for {@link PostgresPagingInterceptor#getCountSql}.
 *
 * <p>No real PostgreSQL is needed: getCountSql only rewrites the SQL text, so these
 * assertions run against the rewritten string directly. Same package as the
 * interceptor to reach the {@code protected} method without a DB round trip.
 */
class PostgresPagingInterceptorTest {

    private final PostgresPagingInterceptor interceptor = new PostgresPagingInterceptor();

    private String count(String sql) {
        return interceptor.getCountSql(sql);
    }

    @Test
    void stripsOrderByAndWrapsCount() {
        String sql = "SELECT id, name FROM users WHERE age > 18 ORDER BY id DESC";
        assertEquals("select count(*) from users where age > 18 ", count(sql));
    }

    @Test
    void stripsLimitOffsetTail() {
        String sql = "SELECT id FROM users WHERE age > 18 LIMIT 10 OFFSET 20";
        assertEquals("select count(*) from users where age > 18 ", count(sql));
    }

    @Test
    void stripsOrderByBeforeLimitWhenBothPresent() {
        // lastIndexOf(order by) wins; the tail (order by ... limit ...) is dropped together.
        String sql = "SELECT id FROM users ORDER BY id DESC LIMIT 10 OFFSET 20";
        assertEquals("select count(*) from users ", count(sql));
    }

    @Test
    void plainSelectGetsCountStar() {
        String sql = "SELECT id, name FROM accounts";
        assertEquals("select count(*) from accounts", count(sql));
    }

    @Test
    void unionIsWrappedInSubquery() {
        String sql = "SELECT id FROM a UNION SELECT id FROM b ORDER BY id";
        assertEquals(
                "select count(*) from ( select id from a union select id from b  ) tmp_inner_union_alias ",
                count(sql));
    }

    @Test
    void unionWithoutTailStillWrapped() {
        String sql = "SELECT id FROM a UNION ALL SELECT id FROM b";
        assertEquals(
                "select count(*) from ( select id from a union all select id from b ) tmp_inner_union_alias ",
                count(sql));
    }
}
