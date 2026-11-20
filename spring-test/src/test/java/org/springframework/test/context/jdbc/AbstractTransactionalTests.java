/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 5.2
 */
@ExtendWith(SpringExtension.class)
@Transactional
public abstract class AbstractTransactionalTests {

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	protected final int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

	protected final void assertNumUsers(int expected) {
		assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
	}

	protected final void assertUsers(String... expectedUsers) {
		List<String> actualUsers = this.jdbcTemplate.queryForList("select name from user", String.class);
		assertThat(actualUsers).containsExactlyInAnyOrder(expectedUsers);
	}

}
