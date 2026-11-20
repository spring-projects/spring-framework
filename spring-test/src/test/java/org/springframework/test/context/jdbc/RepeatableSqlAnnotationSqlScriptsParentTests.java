/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import java.lang.annotation.Repeatable;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * This is a copy of {@link TransactionalSqlScriptsTests} that verifies proper
 * handling of {@link Sql @Sql} as a {@link Repeatable} annotation.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@Sql("schema.sql")
@Sql("data.sql")
@DirtiesContext
class RepeatableSqlAnnotationSqlScriptsParentTests extends AbstractTransactionalTests {

	@Test
	@Order(1)
	void classLevelScripts() {
		assertUsers("Dilbert");
	}

	@Test
	@Sql("drop-schema.sql")
	@Sql("schema.sql")
	@Sql("data.sql")
	@Sql("data-add-dogbert.sql")
	@Order(2)
	void methodLevelScripts() {
		assertUsers("Dilbert", "Dogbert");
	}

}
