package tech.krpc.mybatis.type;

import java.util.List;

/**
 * One row of {@code t_json_strict_matrix}. {@code payload} is a {@code text} column whose value is
 * turned into {@code List<FixtureDto>} by {@link FixtureJsonListHandler}, registered explicitly on
 * the {@code <result>} element of the XML resultMap.
 */
public class MatrixRow {

    private String cell;

    private List<FixtureDto> payload;

    public String getCell() {
        return cell;
    }

    public void setCell(String cell) {
        this.cell = cell;
    }

    public List<FixtureDto> getPayload() {
        return payload;
    }

    public void setPayload(List<FixtureDto> payload) {
        this.payload = payload;
    }
}
