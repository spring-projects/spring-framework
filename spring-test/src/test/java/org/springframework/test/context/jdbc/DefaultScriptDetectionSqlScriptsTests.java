/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests that verify support for default SQL script detection.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@Sql
@DirtiesContext
class DefaultScriptDetectionSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	void classLevel() {
		assertNumUsers(2);
	}

	@Test
	@Sql
	void methodLevel() {
		assertNumUsers(3);
	}

}
