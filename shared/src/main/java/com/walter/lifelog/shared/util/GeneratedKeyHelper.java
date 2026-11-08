package com.walter.lifelog.shared.util;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.conf.ParamType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * jOOQ 로 만든 INSERT 를 실행하고 DB 가 만든 키를 돌려줍니다.
 *
 * <p>jOOQ 의 {@code returning(...)} 을 쓰지 않는 이유가 있습니다. 이 프로젝트는 코드 생성 없이
 * {@code DSL.field("post_seq", Long.class)} 처럼 문자열로 필드를 선언합니다. 그래서 jOOQ 는
 * 어떤 컬럼이 identity 인지 알지 못합니다.
 *
 * <ul>
 *   <li>H2 는 {@code SELECT ... FROM FINAL TABLE (INSERT ...)} 로 에뮬레이션하므로 요청한
 *       이름의 컬럼이 그대로 돌아와 우연히 동작합니다.</li>
 *   <li>MySQL 은 JDBC {@code getGeneratedKeys} 경로인데, jOOQ 가 identity 를 모르는 탓에
 *       요청한 이름으로도 위치로도 값을 꺼낼 수 없습니다. <b>예외 없이 null 이 돌아옵니다.</b></li>
 * </ul>
 *
 * <p>즉 H2 로 하는 테스트는 이 차이를 잡아내지 못합니다. 그래서 양쪽에서 똑같이 동작하는
 * JDBC 표준 생성 키를 직접 씁니다. 바인딩 값은 jOOQ 가 만든 것을 그대로 넘깁니다.
 */
public final class GeneratedKeyHelper {

    private GeneratedKeyHelper() {
    }

    /**
     * @param dsl    jOOQ DSLContext. 호출하는 쪽의 트랜잭션 커넥션을 그대로 씁니다.
     * @param insert 실행할 INSERT. {@code dsl.insertInto(...).set(...)} 형태
     * @return 생성된 키. 키가 돌아오지 않으면 {@link IllegalStateException}
     */
    public static long executeReturningKey(final DSLContext dsl, final Query insert) {
        final String sql = insert.getSQL(ParamType.INDEXED);
        final List<Object> bindValues = insert.getBindValues();

        return dsl.connectionResult(connection -> {
            try (PreparedStatement statement =
                         connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < bindValues.size(); i++) {
                    statement.setObject(i + 1, bindValues.get(i));
                }
                statement.executeUpdate();

                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getLong(1);
                    }
                }
            }
            throw new IllegalStateException("INSERT 후 생성된 키를 받지 못했습니다: " + sql);
        });
    }
}
