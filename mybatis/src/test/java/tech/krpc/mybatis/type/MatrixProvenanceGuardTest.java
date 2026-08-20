package tech.krpc.mybatis.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Bad-sample self-proof for {@link MatrixProvenanceGuard} (EXTMYB-STRICT-001, fix round 1).
 *
 * <p>A provenance guard that has only ever seen good inputs is not evidence. These cases feed it
 * the artifacts it must refuse — above all a published {@code mybatis-<version>.jar}. The
 * {@code :mybatis} project's group is {@code tech.krpc.mybatis}, so its Central-shaped path is
 * {@code …/tech/krpc/mybatis/mybatis/<version>/mybatis-<version>.jar}; that jar really does
 * contain {@code JsonTypeHandler} and {@code AbstractJsonListHandler}, and the first version of
 * this guard — a "path contains {@code ext-mybatis}" blocklist — let it through.
 *
 * <p>Runs in the default {@code test} task, deliberately NOT in {@code strictMatrixTest} /
 * {@code lenientMatrixTest}: those two must report exactly the 11 matrix cells and nothing else.
 *
 * <p>Path comparison only, no filesystem access, so these outcomes do not depend on what happens
 * to be in the developer's local repositories.
 */
class MatrixProvenanceGuardTest {

    private static final String HANDLER = "tech.krpc.mybatis.type.JsonTypeHandler";

    /** Central-shaped location of the published jar that carries both handler classes. */
    private static final String PUBLISHED_MYBATIS_JAR =
            "/repo/tech/krpc/mybatis/mybatis/1.0.2/mybatis-1.0.2.jar";

    /** This worktree's compiled output — the only accepted handler source. */
    private static final Path WORKTREE_CLASSES = Path.of("build", "classes", "java", "main");

    private static final Path RESOLVED_RPC_COMMON =
            Path.of("/repo/tech/krpc/rpc-common/1.2.0/rpc-common-1.2.0.jar");

    private static final String COORDINATE = "tech.krpc:rpc-common:1.2.0";

    // ------------------------------------------------------------------- handler-class binding

    /** The bad sample the old blocklist passed. */
    @Test
    void publishedMybatisJar_isRejectedAsHandlerSource() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> MatrixProvenanceGuard.requireWorktreeClasses(
                        HANDLER, PUBLISHED_MYBATIS_JAR, WORKTREE_CLASSES));
        assertTrue(thrown.getMessage().contains("mybatis-1.0.2.jar"), thrown.getMessage());
    }

    @Test
    void anotherCheckoutsClassesDir_isRejectedAsHandlerSource() {
        assertThrows(IllegalStateException.class,
                () -> MatrixProvenanceGuard.requireWorktreeClasses(HANDLER,
                        "/Users/someone/other-checkout/mybatis/build/classes/java/main/",
                        WORKTREE_CLASSES));
    }

    @Test
    void thisWorktreesClassesDir_isAccepted() {
        // CodeSource locations for directories carry a trailing slash; it must not defeat the match.
        String location = WORKTREE_CLASSES.toAbsolutePath().normalize() + "/";
        assertDoesNotThrow(
                () -> MatrixProvenanceGuard.requireWorktreeClasses(HANDLER, location, WORKTREE_CLASSES));
    }

    /**
     * The old blocklist's other failure mode, inverted: a checkout that merely LIVES under a
     * directory named {@code ext-mybatis} is the expected source and must be accepted.
     */
    @Test
    void aCheckoutUnderAnExtMybatisDirectory_isAccepted() {
        Path expected = Path.of("/Users/someone/ext-mybatis/mybatis/build/classes/java/main");
        assertDoesNotThrow(() -> MatrixProvenanceGuard.requireWorktreeClasses(
                HANDLER, expected + "/", expected));
    }

    // ------------------------------------------------------------------------ decoder binding

    @Test
    void anUnresolvedJar_isRejectedAsDecoderSource() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> MatrixProvenanceGuard.requireResolvedDecoderJar(
                        PUBLISHED_MYBATIS_JAR, RESOLVED_RPC_COMMON, COORDINATE));
        assertTrue(thrown.getMessage().contains("tech.krpc:rpc-common:1.2.0"), thrown.getMessage());
    }

    /** Right module, wrong version: coordinate binding must catch it, not just "it is a jar". */
    @Test
    void aDifferentRpcCommonVersion_isRejectedAsDecoderSource() {
        assertThrows(IllegalStateException.class,
                () -> MatrixProvenanceGuard.requireResolvedDecoderJar(
                        "/repo/tech/krpc/rpc-common/1.1.1/rpc-common-1.1.1.jar",
                        RESOLVED_RPC_COMMON, COORDINATE));
    }

    /**
     * An artifact Gradle did resolve but whose filename does not match the pinned coordinate must
     * still be refused: the guard binds to the coordinate, not only to the resolved path.
     */
    @Test
    void aResolvedJarWithAMismatchedName_isRejectedAsDecoderSource() {
        Path misnamed = Path.of("/repo/tech/krpc/rpc-common/1.2.0/rpc-common-SNAPSHOT.jar");
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> MatrixProvenanceGuard.requireResolvedDecoderJar(
                        misnamed.toString(), misnamed, COORDINATE));
        assertTrue(thrown.getMessage().contains("rpc-common-1.2.0.jar"), thrown.getMessage());
    }

    @Test
    void theResolvedRpcCommonJar_isAccepted() {
        assertDoesNotThrow(() -> MatrixProvenanceGuard.requireResolvedDecoderJar(
                RESOLVED_RPC_COMMON.toString(), RESOLVED_RPC_COMMON, COORDINATE));
    }
}
