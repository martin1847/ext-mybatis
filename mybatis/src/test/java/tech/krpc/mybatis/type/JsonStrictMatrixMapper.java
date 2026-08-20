package tech.krpc.mybatis.type;

import org.apache.ibatis.annotations.Param;

/**
 * Mapper for the decoding matrix. Every cell is a separate row and a separate invocation, i.e. a
 * separate JDBC round-trip and a separate decode, so one cell's rejection can never abort another
 * cell's measurement.
 *
 * <p>The statement and its resultMap (including the {@code typeHandler} attribute) live in
 * {@code mapper/JsonStrictMatrixMapper.xml} — the registration path production uses.
 */
public interface JsonStrictMatrixMapper {

    MatrixRow findByCell(@Param("cell") String cell);
}
