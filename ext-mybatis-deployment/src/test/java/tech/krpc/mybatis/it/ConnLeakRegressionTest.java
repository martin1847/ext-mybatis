package tech.krpc.mybatis.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import jakarta.inject.Inject;

import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Regression guard for the ext-mybatis connection-leak fix (EXTMYB-LEAK-001).
 *
 * <p>Boots a real Quarkus app with Agroal (pool max=4) + the historically-leaking mybatis-config
 * ({@code MANAGED + closeConnection=false + QuarkusDataSourceFactory}, kept verbatim in
 * {@code it-mybatis-config.xml}). The safe-by-construction guard in {@code XmlConfigurationFactory}
 * must force {@code closeConnection=true} so MyBatis returns the pooled connection after each
 * auto-session.
 *
 * <p>Three contracts:
 * <ol>
 *   <li><b>Pool-exhaustion regression</b>: fire {@code >2x} pool-size sequential non-transactional
 *       reads. Pre-fix, a per-request leak exhausts the pool at call {@code max+1} (acquisition
 *       timeout). Post-fix, all reads succeed because each connection is returned.</li>
 *   <li><b>Transaction safety</b>: with an active JTA transaction, {@code closeConnection=true} must
 *       NOT end the transaction early — multi-statement commit atomicity and rollback both hold.</li>
 *   <li><b>Enlisted-connection reuse</b>: multiple mapper calls inside one {@code @Transactional}
 *       method all report the same {@code pg_backend_pid()} — the tx connection is not swapped
 *       mid-transaction by the per-call close.</li>
 * </ol>
 *
 * <p>Requires PostgreSQL on localhost:5433 (the dev IT database, as used by
 * {@code mybatis/PostgresPagingIntegrationTest}); each test self-skips via {@link org.junit.jupiter.api.Assumptions}
 * when the DB is unreachable. The Agroal pool is lazy (min/initial=0) so app boot never needs the DB.
 */
public class ConnLeakRegressionTest {

    private static final String JDBC_URL = "jdbc:postgresql://localhost:5433/krpc_test";
    private static final String JDBC_USER = "krpc";
    private static final String JDBC_PASSWORD = "krpc";
    private static final int POOL_MAX = 4;

    @RegisterExtension
    static final QuarkusUnitTest APP = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Widget.class, WidgetMapper.class, WidgetTxService.class)
                    .addAsResource("it-mybatis-config.xml")
                    .addAsResource("it-mapper/WidgetMapper.xml", "it-mapper/WidgetMapper.xml")
                    .addAsResource("application.properties"))
            // Env-proof: a stray QUARKUS_DATASOURCE_* env var (env-var config source, ordinal 300)
            // otherwise outranks application.properties (ordinal 250) and feeds Agroal the wrong
            // credentials. overrideRuntimeConfigKey routes through RuntimeOverrideConfigSource
            // (ordinal 399 > 300), so these datasource values win over any host-shell env var and the
            // IT datasource is pinned. (overrideConfigKey would NOT suffice — it merges into the
            // archive's application.properties at ordinal 250, still below the env var.)
            .overrideRuntimeConfigKey("quarkus.datasource.username", JDBC_USER)
            .overrideRuntimeConfigKey("quarkus.datasource.password", JDBC_PASSWORD)
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:postgresql://localhost:5433/krpc_test?ApplicationName=extmyb-it-pool");

    @Inject
    WidgetMapper widgetMapper;

    @Inject
    WidgetTxService txService;

    @BeforeEach
    void seed() throws SQLException {
        assumeTrue(canConnect(), "PostgreSQL not reachable on localhost:5433 — skipping");
        try (Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             Statement st = c.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS leak_widget (id int primary key, name text)");
            st.execute("TRUNCATE leak_widget");
            st.execute("INSERT INTO leak_widget (id, name) VALUES (1, 'alice')");
        }
    }

    /**
     * Fire 3x pool-size sequential non-tx reads. A per-request leak would exhaust the max=4 pool at
     * call #5; all 12 succeeding proves each connection is returned (the fix holds).
     */
    @Test
    void nonTransactionalReadsDoNotExhaustPool() {
        int calls = POOL_MAX * 3; // 12
        for (int i = 1; i <= calls; i++) {
            Widget w = widgetMapper.getWidget(1);
            assertEquals("alice", w.getName(),
                    "read #" + i + " must succeed; a failure here means the pool was exhausted by a leak");
        }
    }

    /** JTA commit atomicity: two writes in one transaction are both visible. */
    @Test
    void transactionCommitIsAtomic() throws SQLException {
        txService.saveTwice(101, "tx-a", 102, "tx-b");
        assertTrue(rowExists(101), "row 101 must be visible after commit");
        assertTrue(rowExists(102), "row 102 must be visible after commit");
    }

    /**
     * JTA rollback: closeConnection=true must not end the tx early — a throw rolls the write back.
     */
    @Test
    void transactionRollbackHoldsUnderCloseConnectionTrue() throws SQLException {
        assertThrows(IllegalStateException.class, () -> txService.saveThenThrow(103, "rollback-me"));
        assertFalse(rowExists(103),
                "row 103 must be ROLLED BACK — forced closeConnection=true must not end the JTA tx early");
    }

    /**
     * Same-connection proof: multiple mapper calls inside ONE {@code @Transactional} method must run on
     * the SAME physical connection. {@code pg_backend_pid()} returns the server-side backend PID, unique
     * per connection, so identical PIDs across calls prove the enlisted connection is reused and
     * forced {@code closeConnection=true} does not swap connections mid-transaction.
     */
    @Test
    void mapperCallsInOneTransactionReuseSameConnection() {
        List<Integer> pids = txService.backendPidsInOneTx(3);
        assertEquals(3, pids.size(), "expected one PID per mapper call");
        Integer first = pids.get(0);
        assertTrue(first != null && first > 0, "pg_backend_pid() must return a real backend PID");
        for (int i = 1; i < pids.size(); i++) {
            assertEquals(first, pids.get(i),
                    "call #" + (i + 1) + " ran on a DIFFERENT connection (pid " + pids.get(i)
                            + " != " + first + ") — the enlisted tx connection was not reused");
        }
    }

    private static boolean rowExists(int id) throws SQLException {
        try (Connection c = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
             var ps = c.prepareStatement("select 1 from leak_widget where id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean canConnect() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}
