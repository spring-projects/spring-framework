/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JdbcTestUtils}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 2.5.4
 */
@ExtendWith(MockitoExtension.class)
class JdbcTestUtilsTests {

	@Mock
	Connection connection;

	@Mock
	PreparedStatement preparedStatement;

	@Mock
	JdbcTemplate jdbcTemplate;


	@Test
	void deleteWithoutWhereClause() throws Exception {
		given(connection.prepareStatement("DELETE FROM person")).willReturn(preparedStatement);
		PreparedStatementCreator preparedStatementCreator = assertArg(psc ->
				assertThat(psc.createPreparedStatement(connection)).isSameAs(preparedStatement));
		given(jdbcTemplate.update(preparedStatementCreator)).willReturn(10);

		int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", null);
		assertThat(deleted).isEqualTo(10);
	}

	@Test
	void deleteWithWhereClause() throws Exception {
		given(connection.prepareStatement("DELETE FROM person WHERE name = 'Bob' and age > 25")).willReturn(preparedStatement);
		PreparedStatementCreator preparedStatementCreator = assertArg(psc ->
				assertThat(psc.createPreparedStatement(connection)).isSameAs(preparedStatement));
		given(jdbcTemplate.update(preparedStatementCreator)).willReturn(10);

		int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = 'Bob' and age > 25");
		assertThat(deleted).isEqualTo(10);
	}

	@Test
	void deleteWithWhereClauseAndArguments() throws Exception {
		given(connection.prepareStatement("DELETE FROM person WHERE name = ? and age > ?")).willReturn(preparedStatement);
		PreparedStatementCreator preparedStatementCreator = assertArg(psc -> {
			assertThat(psc.createPreparedStatement(connection)).isSameAs(preparedStatement);
			verify(preparedStatement).setString(1, "Bob");
			verify(preparedStatement).setObject(2, 25);
		});
		given(jdbcTemplate.update(preparedStatementCreator)).willReturn(10);

		int deleted = JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "person", "name = ? and age > ?", "Bob", 25);
		assertThat(deleted).isEqualTo(10);
	}

}
