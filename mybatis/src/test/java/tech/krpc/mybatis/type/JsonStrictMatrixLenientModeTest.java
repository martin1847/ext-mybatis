package tech.krpc.mybatis.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * EXTMYB-STRICT-001 — the same 11 cells with the kill switch thrown: {@code KRPC_JSON_STRICT=false}.
 * Run by the {@code lenientMatrixTest} Gradle task.
 *
 * <p>Claim boundary: what this class measures is the behaviour of rpc-common 1.2.0 with the kill
 * switch thrown. The krpc 1.2.0 CHANGELOG (#56) describes that state as the pre-1.2 behaviour
 * ("Rollback needs no code change: {@code KRPC_JSON_STRICT=false}"), but rpc-common 1.0.3 was
 * never run here, so pre-1.2 equivalence is a cited contract, not a measurement. These results
 * establish a strict-vs-lenient DIFFERENCE on one pinned rpc-common — not an upgrade regression.
 *
 * <p>SNAPSHOT CHARACTERIZATION — these expectations record today's behaviour, not a promise.
 */
class JsonStrictMatrixLenientModeTest extends AbstractJsonStrictMatrixTest {

    /**
     * Literal mode guard; see the strict sibling for why it is not a shared parameterized helper.
     */
    @BeforeAll
    static void modeGuard() {
        assertEquals("false", System.getenv("KRPC_JSON_STRICT"),
                "lenientMatrixTest must fork with KRPC_JSON_STRICT=false");
        assertFalse(observedStrict(), "the decoder resolved by this JVM must be the lenient one");
    }

    // ------------------------------------------------------------------- harmful candidates

    /**
     * KNOWN-POSITIVE GATE, other half: the row the strict task refuses is accepted here and the
     * number is silently stringified. Same commit, same row, same query, two outcomes.
     */
    @Test
    void integerIntoStringField_silentlyStringified() {
        assertEquals("123", acceptedElement("integer_to_string").legacyId);
    }

    @Test
    void floatIntoStringField_silentlyStringified() {
        assertEquals("1.5", acceptedElement("float_to_string").ratio);
    }

    @Test
    void booleanIntoStringField_silentlyStringified() {
        assertEquals("true", acceptedElement("boolean_to_string").flag);
    }

    /** Unchanged by the switch: the guard never covered non-Textual targets. */
    @Test
    void stringIntoIntegerField_coerced() {
        assertEquals(42, acceptedElement("string_to_integer").count);
    }

    @Test
    void stringIntoDoubleField_coerced() {
        assertEquals(3.14d, acceptedElement("string_to_double").amount);
    }

    @Test
    void floatIntoIntegerField_truncated() {
        assertEquals(1, acceptedElement("float_to_integer").count);
    }

    // ---------------------------------------------------------------------- negative controls

    @Test
    void stringIntoStringField_accepted() {
        assertEquals("abc", acceptedElement("string_to_string").name);
    }

    @Test
    void integerIntoIntegerField_accepted() {
        assertEquals(42, acceptedElement("integer_to_integer").count);
    }

    @Test
    void floatIntoDoubleField_accepted() {
        assertEquals(2.5d, acceptedElement("float_to_double").amount);
    }

    @Test
    void booleanIntoBooleanField_accepted() {
        assertTrue(acceptedElement("boolean_to_boolean").enabled);
    }

    @Test
    void nullIntoNullableField_accepted() {
        assertNull(acceptedElement("null_to_nullable").name);
    }
}
