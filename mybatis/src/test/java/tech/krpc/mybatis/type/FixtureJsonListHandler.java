package tech.krpc.mybatis.type;

/**
 * The ONLY fixture type handler of EXTMYB-STRICT-001: it binds the generic parameter and nothing
 * else.
 *
 * <p>Deliberately empty. Overriding {@code parseJSON} here would replace the very code under
 * measurement, so the decoding path stays exactly the production one:
 * {@code resultMap -> JsonTypeHandler.getNullableResult -> AbstractJsonListHandler.parseJSON
 * -> JsonUtils.parse(String, Type)}.
 *
 * <p>Consequence for the claim boundary: the matrix measures the {@link AbstractJsonListHandler}
 * family only. {@link JsonTypeHandler} itself never calls krpc — a sibling subclass with a
 * different {@code parseJSON} is NOT covered by these results.
 */
public class FixtureJsonListHandler extends AbstractJsonListHandler<FixtureDto> {
}
