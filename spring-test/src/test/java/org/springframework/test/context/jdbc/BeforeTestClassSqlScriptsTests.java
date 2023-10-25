/*
 * Copyright 2002-2023 the original author or authors.
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
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

/**
 * Verifies that {@link Sql @Sql} with {@link ExecutionPhase#BEFORE_TEST_CLASS}
 * is run before all tests in the class have been run.
 *
 * @author Andreas Ahlenstorf
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@DirtiesContext(classMode = BEFORE_CLASS)
@Sql(scripts = {"recreate-schema.sql", "data-add-catbert.sql"}, executionPhase = BEFORE_TEST_CLASS)
class BeforeTestClassSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	void classLevelScriptsHaveBeenRun() {
		assertUsers("Catbert");
	}

	@Test
	@Sql("data-add-dogbert.sql")
	@SqlMergeMode(MERGE)
	void mergeDoesNotAffectClassLevelPhase() {
		assertUsers("Catbert", "Dogbert");
	}

	@Test
	@Sql({"data-add-dogbert.sql"})
	@SqlMergeMode(OVERRIDE)
	void overrideDoesNotAffectClassLevelPhase() {
		assertUsers("Catbert", "Dogbert");
	}

	@Nested
	class NestedBeforeTestClassSqlScriptsTests {

		@Test
		void classLevelScriptsHaveBeenRun() {
			assertUsers("Catbert");
		}

		@Test
		@Sql("data-add-dogbert.sql")
		@SqlMergeMode(MERGE)
		void mergeDoesNotAffectClassLevelPhase() {
			assertUsers("Catbert", "Dogbert");
		}

		@Test
		@Sql({"data-add-dogbert.sql"})
		@SqlMergeMode(OVERRIDE)
		void overrideDoesNotAffectClassLevelPhase() {
			assertUsers("Catbert", "Dogbert");
		}

	}

}
