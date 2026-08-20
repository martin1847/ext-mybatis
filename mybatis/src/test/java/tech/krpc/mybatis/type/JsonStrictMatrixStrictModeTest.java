package tech.krpc.mybatis.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * EXTMYB-STRICT-001 — the 11-cell matrix under the DEFAULT switch state (strict decoding), i.e.
 * {@code KRPC_JSON_STRICT} explicitly ABSENT from the environment. Run by the {@code
 * strictMatrixTest} Gradle task; see {@link AbstractJsonStrictMatrixTest} for why the two switch
 * states need two forked JVMs.
 *
 * <p>SNAPSHOT CHARACTERIZATION — these expectations record today's behaviour, not a promise.
 */
class JsonStrictMatrixStrictModeTest extends AbstractJsonStrictMatrixTest {

    @BeforeAll
    static void modeGuard() {
        requireMode(true);
    }

    // ------------------------------------------------------------------- harmful candidates

    /**
     * KNOWN-POSITIVE GATE. Legacy rows holding {@code [{"legacyId":123}]} for a {@code String}
     * field decoded fine before 1.2.0 and are REFUSED now. The lenient sibling of this test still
     * accepts the same row, so the two states differ — the instrument can see the damage.
     */
    @Test
    void integerIntoStringField_rejected() {
        rejected("integer_to_string");
    }

    @Test
    void floatIntoStringField_rejected() {
        rejected("float_to_string");
    }

    @Test
    void booleanIntoStringField_rejected() {
        rejected("boolean_to_string");
    }

    /** Strict decoding constrains Textual TARGETS only: a JSON string into a number still coerces. */
    @Test
    void stringIntoIntegerField_stillCoerced() {
        assertEquals(42, acceptedElement("string_to_integer").count);
    }

    @Test
    void stringIntoDoubleField_stillCoerced() {
        assertEquals(3.14d, acceptedElement("string_to_double").amount);
    }

    /** {@code ACCEPT_FLOAT_AS_INT} is untouched by the guard: 1.5 silently truncates to 1. */
    @Test
    void floatIntoIntegerField_stillTruncated() {
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
