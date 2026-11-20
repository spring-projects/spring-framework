/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.aot.DisabledInAotMode;

/**
 * Integration tests that verify support for property placeholders in SQL script locations.
 *
 * @author Sam Brannen
 * @since 6.2
 */
class PropertyPlaceholderSqlScriptsTests {

	private static final String SCRIPT_LOCATION = "classpath:org/springframework/test/context/jdbc/${vendor}/data.sql";

	@Nested
	@ContextConfiguration(classes = PopulatedSchemaDatabaseConfig.class)
	@TestPropertySource(properties = "vendor = db1")
	@DirtiesContext
	@DisabledInAotMode("${vendor} does not get resolved during AOT processing")
	class DatabaseOneTests extends AbstractTransactionalTests {

		@Test
		@Sql(SCRIPT_LOCATION)
		void placeholderIsResolvedInScriptLocation() {
			assertUsers("Dilbert 1");
		}
	}

	@Nested
	@ContextConfiguration(classes = PopulatedSchemaDatabaseConfig.class)
	@TestPropertySource(properties = "vendor = db2")
	@DirtiesContext
	@DisabledInAotMode("${vendor} does not get resolved during AOT processing")
	class DatabaseTwoTests extends AbstractTransactionalTests {

		@Test
		@Sql(SCRIPT_LOCATION)
		void placeholderIsResolvedInScriptLocation() {
			assertUsers("Dilbert 2");
		}
	}

}
