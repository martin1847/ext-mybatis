package tech.krpc.ext.it;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import tech.krpc.mybatis.DbBounds;
import tech.krpc.ext.it.dto.Widget;
import tech.krpc.ext.it.mapper.WidgetMapper;

/**
 * ext-mybatis native-image smoke — command mode. Boots the Quarkus app (in CI as a GraalVM
 * native binary), seeds a real PostgreSQL table via the injected Agroal {@link DataSource}, then
 * drives the CDI-injected {@link WidgetMapper} to prove, at native runtime, that:
 *
 * <ol>
 *   <li>the mapper interface is a working CDI bean + native JDK proxy (injection + call succeed);</li>
 *   <li>{@link Widget} is registered for native reflection (result rows populate — non-null,
 *       correct field VALUES, not merely a non-empty list);</li>
 *   <li>the mapper XML resource is embedded in the image and its statements bind;</li>
 *   <li>the {@code PostgresPagingInterceptor} count-rewrite + offset/limit round-trips against a
 *       real DB (page content AND total count are exact).</li>
 * </ol>
 *
 * Assertions are STRONG on purpose (exact field values / exact count): a dropped reflection
 * registration during native augmentation makes rows come back null or the query throw, which
 * flips a PASS to a FAIL and a non-zero exit. Every check prints a grep-able marker so CI can
 * assert on the actual seeded values, not just the exit code.
 */
@QuarkusMain
public class NativeItMain implements QuarkusApplication {

    private static final Logger LOG = Logger.getLogger(NativeItMain.class);

    private static final String TABLE = "t_native_it";

    @Inject
    WidgetMapper widgetMapper;

    @Inject
    DataSource dataSource;

    @Override
    public int run(String... args) throws Exception {
        try {
            seed();
            boolean ok = checkListAll() & checkPaged();
            if (ok) {
                LOG.info("NATIVE-IT ALL PASS");
                return 0;
            }
            LOG.error("NATIVE-IT FAIL: one or more checks failed");
            return 1;
        } catch (Exception e) {
            // A native reflection gap typically surfaces here (mapper proxy / DTO instantiation).
            LOG.error("NATIVE-IT FAIL: exception during mapper round-trip", e);
            return 1;
        } finally {
            dropQuietly();
        }
    }

    /** Fresh, deterministic fixture: drop + create + seed 5 known rows via the real datasource. */
    private void seed() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("drop table if exists " + TABLE);
            st.execute("create table " + TABLE
                    + " (id integer primary key, name varchar(64) not null, qty integer not null)");
            st.execute("insert into " + TABLE + " (id, name, qty) values "
                    + "(1,'alpha',10),(2,'bravo',20),(3,'charlie',30),(4,'delta',40),(5,'echo',50)");
            if (!conn.getAutoCommit()) {
                conn.commit();
            }
        }
        LOG.info("NATIVE-IT seeded " + TABLE + " with 5 rows");
    }

    /** Baseline mapper + DTO round-trip: all 5 rows, exact field values on the ends. */
    private boolean checkListAll() {
        List<Widget> all = widgetMapper.listAll();
        LOG.info("NATIVE-IT listAll -> " + render(all));
        if (all.size() != 5) {
            LOG.error("NATIVE-IT FAIL listAll: expected 5 rows, got " + all.size());
            return false;
        }
        Widget first = all.get(0);
        Widget last = all.get(4);
        boolean ok = eq(first, 1, "alpha", 10) && eq(last, 5, "echo", 50);
        if (ok) {
            LOG.info("NATIVE-IT PASS listAll: 5 widgets, id1=alpha(10), id5=echo(50)");
        } else {
            LOG.error("NATIVE-IT FAIL listAll: field values did not match (first=" + first + ", last=" + last + ")");
        }
        return ok;
    }

    /** Paging interceptor round-trip: page 2 size 2 -> rows id 3,4; count-rewrite -> total 5. */
    private boolean checkPaged() {
        DbBounds bounds = DbBounds.fromPage(2, 2); // offset 2, limit 2, needCount
        List<Widget> page = widgetMapper.listPaged(new HashMap<>(), bounds);
        LOG.info("NATIVE-IT paged(page2,size2) -> " + render(page) + ", count=" + bounds.getCount());
        if (page.size() != 2) {
            LOG.error("NATIVE-IT FAIL paged: expected 2 rows, got " + page.size());
            return false;
        }
        boolean rows = eq(page.get(0), 3, "charlie", 30) && eq(page.get(1), 4, "delta", 40);
        boolean count = bounds.getCount() == 5;
        if (rows && count) {
            LOG.info("NATIVE-IT PASS paged: page2size2 -> id3=charlie(30), id4=delta(40); count=5");
            return true;
        }
        LOG.error("NATIVE-IT FAIL paged: rows/count mismatch (rowsOk=" + rows + ", count=" + bounds.getCount() + ")");
        return false;
    }

    private static boolean eq(Widget w, int id, String name, int qty) {
        return w != null
                && w.getId() != null && w.getId() == id
                && name.equals(w.getName())
                && w.getQty() != null && w.getQty() == qty;
    }

    private static String render(List<Widget> ws) {
        return ws.stream().map(Widget::toString).collect(Collectors.joining(", ", "[", "]"));
    }

    private void dropQuietly() {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("drop table if exists " + TABLE);
        } catch (Exception e) {
            LOG.warn("NATIVE-IT teardown: could not drop " + TABLE + " (" + e.getMessage() + ")");
        }
    }
}
