package tech.krpc.mybatis.type;

import java.nio.file.Path;

/**
 * EXTMYB-STRICT-001 — the provenance guard for the decoding matrix, as pure predicates so the
 * guard itself can be fed a bad sample (see {@code MatrixProvenanceGuardTest}).
 *
 * <p><b>Positive binding, not a blocklist.</b> The first version of this guard rejected a
 * {@code CodeSource} whose path merely <i>contained</i> {@code "ext-mybatis"}. That fails open:
 * the handler classes are published by the {@code :mybatis} project under group
 * {@code tech.krpc.mybatis}, so the Central-shaped path is
 * {@code …/tech/krpc/mybatis/mybatis/<version>/mybatis-<version>.jar}, which contains no
 * {@code ext-mybatis} substring and would have passed. It also fails closed for the wrong reason:
 * any checkout living under a directory named {@code ext-mybatis} would have been rejected. Both
 * methods below therefore assert an EXACT expected location instead of guessing at a bad one.
 *
 * <p>No filesystem access: comparisons are on normalized absolute paths, so the predicates are
 * deterministic and can be exercised with paths that do not exist on the running machine.
 */
final class MatrixProvenanceGuard {

    private MatrixProvenanceGuard() {
    }

    /**
     * The production handler classes MUST load from this worktree's own {@code :mybatis} compiled
     * output — the directory Gradle just compiled, resolved from the test's working directory (the
     * {@code mybatis} project dir). Anything else — a Central {@code mybatis-<version>.jar}, a
     * stale local jar, a jar from another checkout — is a different tree and a different
     * measurement.
     *
     * @param className the class being vouched for, for the error message
     * @param location  its {@code CodeSource} location path
     * @param expected  the only accepted location (this worktree's {@code build/classes/java/main})
     */
    static void requireWorktreeClasses(String className, String location, Path expected) {
        Path actual = normalize(location);
        Path want = expected.toAbsolutePath().normalize();
        if (!actual.equals(want)) {
            throw new IllegalStateException("EXTMYB-STRICT-001: " + className + " must load from this"
                    + " worktree's compiled output " + want + ", but loaded from " + actual
                    + " — the matrix would be measuring a different tree");
        }
    }

    /**
     * {@code JsonUtils} MUST load from the exact jar Gradle resolved for this run, and that jar
     * MUST carry the pinned coordinate's filename. {@code expectedJar} is injected by the Gradle
     * test task from {@code configurations.testRuntimeClasspath} — i.e. the same resolution
     * {@code dependencyInsight} reports — so "the class we loaded" is bound to "the artifact the
     * build resolved", not merely to "some jar".
     *
     * @param location    the {@code CodeSource} location path of {@code JsonUtils}
     * @param expectedJar the rpc-common artifact Gradle resolved onto the test runtime classpath
     * @param coordinate  the pinned coordinate, {@code group:module:version}
     */
    static void requireResolvedDecoderJar(String location, Path expectedJar, String coordinate) {
        String[] gav = coordinate.split(":");
        if (gav.length != 3) {
            throw new IllegalStateException("EXTMYB-STRICT-001: expected a group:module:version"
                    + " coordinate, got " + coordinate);
        }
        String expectedFileName = gav[1] + "-" + gav[2] + ".jar";
        Path actual = normalize(location);
        Path want = expectedJar.toAbsolutePath().normalize();
        if (!actual.equals(want)) {
            throw new IllegalStateException("EXTMYB-STRICT-001: JsonUtils must load from the artifact"
                    + " Gradle resolved for " + coordinate + " (" + want + "), but loaded from "
                    + actual);
        }
        if (!actual.getFileName().toString().equals(expectedFileName)) {
            throw new IllegalStateException("EXTMYB-STRICT-001: the resolved decoder artifact is not"
                    + " named " + expectedFileName + " — coordinate " + coordinate + " does not match "
                    + actual);
        }
    }

    /**
     * A {@code CodeSource} location is a URL path: directories carry a trailing slash and may be
     * percent-encoded on paths with spaces. Normalizing drops the trailing slash so a directory and
     * its slash-less spelling compare equal.
     */
    private static Path normalize(String location) {
        return Path.of(location.replace("%20", " ")).toAbsolutePath().normalize();
    }
}
