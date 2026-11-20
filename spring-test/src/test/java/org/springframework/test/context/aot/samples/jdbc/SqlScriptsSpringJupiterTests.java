/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot.samples.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.EmptyDatabaseConfig;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@Transactional
@SqlMergeMode(MERGE)
@Sql("/org/springframework/test/context/jdbc/schema.sql")
@DirtiesContext
@TestPropertySource(properties = "test.engine = jupiter")
public class SqlScriptsSpringJupiterTests {

	@Test
	@Sql // default script --> org/springframework/test/context/aot/samples/jdbc/SqlScriptsSpringJupiterTests.test.sql
	void test(@Autowired JdbcTemplate jdbcTemplate) {
		assertThat(countRowsInTable(jdbcTemplate, "user")).isEqualTo(1);
	}

}
