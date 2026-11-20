/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests which verify that scripts executed via {@link Sql @Sql}
 * will persist between non-transactional test methods.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql({ "schema.sql", "data.sql" })
@DirtiesContext
class NonTransactionalSqlScriptsTests {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Test
	@Order(1)
	void classLevelScripts() {
		assertNumUsers(1);
	}

	@Test
	@Sql("data-add-dogbert.sql")
	@Order(2)
	void methodLevelScripts() {
		assertNumUsers(2);
	}

	void assertNumUsers(int expected) {
		assertThat(JdbcTestUtils.countRowsInTable(jdbcTemplate, "user")).as("Number of rows in the 'user' table.").isEqualTo(expected);
	}

}
