/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.config.interfaces;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.3
 */
@ExtendWith(SpringExtension.class)
class SqlConfigInterfaceTests implements SqlConfigTestInterface {

	JdbcTemplate jdbcTemplate;

	@Autowired
	void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Test
	@Sql(scripts = "/org/springframework/test/context/jdbc/schema.sql", //
			config = @SqlConfig(separator = ";"))
	@Sql("/org/springframework/test/context/jdbc/data-add-users-with-custom-script-syntax.sql")
	void methodLevelScripts() {
		assertNumUsers(3);
	}

	void assertNumUsers(int expected) {
		assertThat(countRowsInTable("user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
	}

	int countRowsInTable(String tableName) {
		return JdbcTestUtils.countRowsInTable(this.jdbcTemplate, tableName);
	}

}
