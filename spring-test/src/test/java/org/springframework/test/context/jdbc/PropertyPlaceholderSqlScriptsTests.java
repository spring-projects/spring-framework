/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
