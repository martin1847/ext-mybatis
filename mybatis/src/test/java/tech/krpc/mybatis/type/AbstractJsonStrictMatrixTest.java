package tech.krpc.mybatis.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import tech.krpc.util.JsonDecodeException;
import tech.krpc.util.JsonUtils;

/**
 * EXTMYB-STRICT-001 — shared rig for the JSON strict-decoding behaviour matrix.
 *
 * <p><b>SNAPSHOT CHARACTERIZATION, NOT A PRODUCT CONTRACT.</b> Every expectation in the two
 * concrete subclasses records what this ext-mybatis commit + its pinned {@code rpc-common} ACTUALLY
 * do today, including outcomes that are arguably wrong. A future goal that fixes behaviour is
 * expected to UPDATE these expectations; a red test here means "behaviour moved", not necessarily
 * "behaviour broke".
 *
 * <p><b>Why two subclasses instead of one parameterized class.</b> {@code JsonUtils} resolves the
 * {@code KRPC_JSON_STRICT} kill switch once and caches it in a private static field, and Jackson
 * mappers cannot be reconfigured after first use. One JVM can therefore observe exactly ONE switch
 * state. {@link JsonStrictMatrixStrictModeTest} and {@link JsonStrictMatrixLenientModeTest} hold
 * the two literal expectation sets and are run by two separate forked Gradle test tasks
 * ({@code strictMatrixTest} / {@code lenientMatrixTest}) with explicitly-controlled environments.
 * Nothing here reflects into the private cache and nothing depends on execution order.
 *
 * <p><b>The database is a hard precondition, never an assumption.</b> If PostgreSQL is unreachable
 * the rig fails the run; it does not skip. A skipped matrix measures nothing.
 */
abstract class AbstractJsonStrictMatrixTest {

    /** Same instance the repo's other real-DB test uses (docker: {@code postgres:17} on 5433). */
    private static final String JDBC_URL = "jdbc:postgresql://localhost:5433/krpc_test";
    private static final String JDBC_USER = "krpc";
    private static final String JDBC_PASSWORD = "krpc";

    private static final String TABLE = "t_json_strict_matrix";
    private static final String MAPPER_RESOURCE = "mapper/JsonStrictMatrixMapper.xml";

    /** cell key -> the JSON text stored in the {@code payload} column of that cell's row. */
    static final Map<String, String> CELLS = new LinkedHashMap<>();

    static {
        // --- harmful candidates -------------------------------------------------------------
        CELLS.put("integer_to_string", "[{\"legacyId\":123}]");
        CELLS.put("float_to_string", "[{\"ratio\":1.5}]");
        CELLS.put("boolean_to_string", "[{\"flag\":true}]");
        CELLS.put("string_to_integer", "[{\"count\":\"42\"}]");
        CELLS.put("string_to_double", "[{\"amount\":\"3.14\"}]");
        CELLS.put("float_to_integer", "[{\"count\":1.5}]");
        // --- negative controls (must stay green in BOTH switch states) ----------------------
        CELLS.put("string_to_string", "[{\"name\":\"abc\"}]");
        CELLS.put("integer_to_integer", "[{\"count\":42}]");
        CELLS.put("float_to_double", "[{\"amount\":2.5}]");
        CELLS.put("boolean_to_boolean", "[{\"enabled\":true}]");
        CELLS.put("null_to_nullable", "[{\"name\":null}]");
    }

    private static SqlSessionFactory sqlSessionFactory;

    /** Provenance + observations, dumped to {@code build/strict-matrix-evidence/} in {@link #tearDown}. */
    private static final Map<String, String> EVIDENCE = new LinkedHashMap<>();

    @BeforeAll
    static void bootstrapRig() throws Exception {
        seedDatabase();
        sqlSessionFactory = buildSqlSessionFactory();
        EVIDENCE.putAll(provenance());
        EVIDENCE.forEach((k, v) -> System.out.println("[EXTMYB-STRICT-001] " + k + '=' + v));
    }

    @AfterAll
    static void tearDown() throws Exception {
        writeEvidence(EVIDENCE);
        try (Connection conn = connect(); Statement st = conn.createStatement()) {
            st.execute("drop table if exists " + TABLE);
        }
    }

    // ------------------------------------------------------------------ cell access + asserts

    /** Runs THIS cell's own query and returns the decoded payload. */
    private static List<FixtureDto> decode(String cell) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(JsonStrictMatrixMapper.class).findByCell(cell).getPayload();
        }
    }

    /** The cell decodes; returns its single element. */
    static FixtureDto acceptedElement(String cell) {
        List<FixtureDto> payload = decode(cell);
        assertEquals(1, payload.size(), cell + ": fixture rows hold exactly one JSON element");
        return payload.get(0);
    }

    /**
     * The cell is REFUSED on the framework read path. MyBatis wraps a type-handler failure twice
     * ({@code PersistenceException -> ResultMapException -> ...}), so the assertion pins the
     * outer type and then the krpc-typed decode failure inside the cause chain.
     */
    static void rejected(String cell) {
        PersistenceException thrown = assertThrows(PersistenceException.class, () -> decode(cell));
        JsonDecodeException decodeFailure = causeOfType(thrown, JsonDecodeException.class);
        assertInstanceOf(JsonDecodeException.class, decodeFailure,
                cell + ": expected a krpc JsonDecodeException in the cause chain of " + thrown);
        assertEquals("malformed JSON: cannot decode request body", decodeFailure.getMessage());
        // The verbatim refusal, for the findings report: the sanitized krpc message plus the
        // Jackson root cause it hides (server-side only, one line).
        Throwable root = decodeFailure.getCause();
        EVIDENCE.put("reject." + cell, decodeFailure.getClass().getName() + ": "
                + decodeFailure.getMessage() + " <- " + root.getClass().getName() + ": "
                + String.valueOf(root.getMessage()).replace('\n', ' '));
    }

    private static <T extends Throwable> T causeOfType(Throwable thrown, Class<T> type) {
        for (Throwable t = thrown; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return null;
    }

    /**
     * Guard wired from each concrete subclass: the switch state this expectation set was written
     * for must be the state the JVM actually resolved. Asserts the raw environment variable AND
     * the observed decoder behaviour, so a task whose env wiring silently regressed cannot pass
     * by luck.
     */
    static void requireMode(boolean expectStrict) {
        assertEquals(expectStrict ? null : "false", System.getenv(JsonUtils.STRICT_TEXTUAL_COERCION_ENV),
                "KRPC_JSON_STRICT as seen by this forked JVM");
        assertEquals(expectStrict, observedStrict(), "decoder behaviour observed via JsonUtils");
    }

    /**
     * Behaviour probe of the resolved kill switch: an integer into a String field is the one shape
     * strict decoding refuses. Not an assertion — the evidence file records it and
     * {@link #requireMode} asserts it.
     */
    private static boolean observedStrict() {
        try {
            JsonUtils.parse("{\"legacyId\":1}", FixtureDto.class);
            return false;
        } catch (JsonDecodeException e) {
            return true;
        }
    }

    // ------------------------------------------------------------------------------ the rig

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    private static void seedDatabase() throws SQLException {
        Connection conn;
        try {
            conn = connect();
        } catch (SQLException e) {
            throw new IllegalStateException("EXTMYB-STRICT-001: PostgreSQL unreachable at " + JDBC_URL
                    + " — the decoding matrix is a real-DB measurement and MUST NOT be skipped. Start it with:"
                    + " docker run -d --name krpc-test-pg -p 5433:5432 -e POSTGRES_USER=krpc"
                    + " -e POSTGRES_PASSWORD=krpc -e POSTGRES_DB=krpc_test postgres:17", e);
        }
        try (Connection c = conn; Statement st = c.createStatement()) {
            st.execute("drop table if exists " + TABLE);
            st.execute("create table " + TABLE
                    + " (id serial primary key, cell varchar(64) not null unique, payload text)");
            for (Map.Entry<String, String> cell : CELLS.entrySet()) {
                st.execute("insert into " + TABLE + " (cell, payload) values ('" + cell.getKey()
                        + "', '" + cell.getValue() + "')");
            }
        }
    }

    private static SqlSessionFactory buildSqlSessionFactory() throws IOException {
        DataSource dataSource =
                new UnpooledDataSource("org.postgresql.Driver", JDBC_URL, JDBC_USER, JDBC_PASSWORD);
        Configuration configuration =
                new Configuration(new Environment("matrix", new JdbcTransactionFactory(), dataSource));
        // The XML carries the resultMap and its typeHandler attribute; parsing it also binds the
        // mapper interface named by the namespace.
        try (InputStream in = resource(MAPPER_RESOURCE)) {
            new XMLMapperBuilder(in, configuration, MAPPER_RESOURCE, configuration.getSqlFragments()).parse();
        }
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    private static InputStream resource(String name) throws IOException {
        InputStream in = AbstractJsonStrictMatrixTest.class.getClassLoader().getResourceAsStream(name);
        if (in == null) {
            throw new IOException("fixture resource missing from the test classpath: " + name);
        }
        return in;
    }

    // ------------------------------------------------------------------------------ provenance

    /**
     * Premise evidence, asserted as a hard precondition and dumped to
     * {@code build/strict-matrix-evidence/}: the three classes on the measured path must come from
     * THIS worktree's {@code :mybatis} output plus the pinned {@code rpc-common} jar, and
     * {@code JsonUtils} must appear exactly once on the classpath. A published ext-mybatis artifact
     * anywhere on that path would mean the matrix measured Central, not this tree.
     */
    private static Map<String, String> provenance() throws Exception {
        Map<String, String> facts = new LinkedHashMap<>();
        facts.put("mode", System.getProperty("strict.matrix.mode", "<unset>"));
        facts.put("pid", String.valueOf(ProcessHandle.current().pid()));
        String env = System.getenv(JsonUtils.STRICT_TEXTUAL_COERCION_ENV);
        facts.put("env." + JsonUtils.STRICT_TEXTUAL_COERCION_ENV, env == null ? "<unset>" : "\"" + env + "\"");
        facts.put("observedStrict", String.valueOf(observedStrict()));

        for (Class<?> type : List.of(JsonTypeHandler.class, AbstractJsonListHandler.class, JsonUtils.class)) {
            String location = codeSource(type);
            if (location.contains("ext-mybatis")) {
                throw new IllegalStateException("EXTMYB-STRICT-001: " + type.getName() + " loaded from a"
                        + " published ext-mybatis artifact (" + location + ") — the matrix must measure"
                        + " this worktree's :mybatis output, not Central");
            }
            facts.put("codeSource." + type.getSimpleName(), location);
        }
        String jsonUtilsJar = codeSource(JsonUtils.class);
        if (!jsonUtilsJar.endsWith(".jar")) {
            throw new IllegalStateException("EXTMYB-STRICT-001: JsonUtils must come from the pinned"
                    + " rpc-common jar, got " + jsonUtilsJar);
        }
        facts.put("sha256.rpc-common.jar", sha256(Path.of(jsonUtilsJar)));

        List<String> duplicates = classpathHits("tech/krpc/util/JsonUtils.class");
        if (duplicates.size() != 1) {
            throw new IllegalStateException("EXTMYB-STRICT-001: expected exactly one JsonUtils.class on"
                    + " the test classpath, found " + duplicates);
        }
        facts.put("classpathHits.JsonUtils.class", duplicates.toString());
        // jackson-databind is a RUNTIME-scope dependency of rpc-common, so it is absent from the
        // test COMPILE classpath: resolve it reflectively and record the jar it actually loaded
        // from (a stronger fact than a manifest string anyway).
        facts.put("codeSource.jackson-databind",
                codeSource(Class.forName("com.fasterxml.jackson.databind.ObjectMapper")));
        facts.put("codeSource.mybatis", codeSource(Configuration.class));
        facts.put("java.version", System.getProperty("java.version"));
        return facts;
    }

    private static String codeSource(Class<?> type) {
        return type.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private static List<String> classpathHits(String resource) throws IOException {
        List<String> hits = new ArrayList<>();
        for (URL url : Collections.list(
                AbstractJsonStrictMatrixTest.class.getClassLoader().getResources(resource))) {
            hits.add(url.toString());
        }
        return hits;
    }

    private static void writeEvidence(Map<String, String> facts) throws IOException {
        StringBuilder text = new StringBuilder();
        facts.forEach((k, v) -> text.append(k).append('=').append(v).append('\n'));
        Path dir = Path.of("build", "strict-matrix-evidence");
        Files.createDirectories(dir);
        Path file = dir.resolve(facts.get("mode") + "-evidence.txt");
        Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
        System.out.println("[EXTMYB-STRICT-001] evidence written to " + file.toAbsolutePath());
    }
}
