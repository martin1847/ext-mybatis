package tech.krpc.mybatis.type;

/**
 * The single DTO every matrix cell decodes into (EXTMYB-STRICT-001).
 *
 * <p>One DTO, not one per cell: {@code JsonUtils}' mapper has
 * {@code FAIL_ON_UNKNOWN_PROPERTIES=false} and each cell's JSON sets exactly ONE field, so the
 * target-type dimension of the matrix is expressed by WHICH field a cell's JSON key hits. Public
 * fields (Jackson auto-detects them) keep the fixture free of 14 accessor methods; MyBatis never
 * touches this type — it only ever sees {@link MatrixRow}.
 */
public class FixtureDto {

    /** Target of the {@code integer -> String} cell (the known-positive gate). */
    public String legacyId;

    /** Target of the {@code float -> String} cell. */
    public String ratio;

    /** Target of the {@code boolean -> String} cell. */
    public String flag;

    /** Target of the {@code string -> Integer}, {@code float -> Integer}, {@code integer -> Integer} cells. */
    public Integer count;

    /** Target of the {@code string -> Double} and {@code float -> Double} cells. */
    public Double amount;

    /** Target of the {@code string -> String} and {@code null -> nullable} cells. */
    public String name;

    /** Target of the {@code boolean -> Boolean} cell. */
    public Boolean enabled;
}
