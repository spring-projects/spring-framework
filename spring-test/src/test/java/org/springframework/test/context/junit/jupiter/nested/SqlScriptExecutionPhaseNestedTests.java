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

package org.springframework.test.context.junit.jupiter.nested;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.EmptyDatabaseConfig;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.springframework.test.annotation.DirtiesContext.ClassMode.BEFORE_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;

/**
 * Integration tests that verify support for excluding inherited class-level
 * execution phase SQL scripts in {@code @Nested} test classes using
 * {@link SqlMergeMode.MergeMode#OVERRIDE_AND_EXCLUDE_INHERITED_EXECUTION_PHASE_SCRIPTS}.
 *
 * <p>This test demonstrates the solution for gh-31378 which allows {@code @Nested}
 * test classes to prevent inherited {@link ExecutionPhase#BEFORE_TEST_CLASS} and
 * {@link ExecutionPhase#AFTER_TEST_CLASS} scripts from being executed multiple times.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see SqlScriptNestedTests
 * @see BeforeTestClassSqlScriptsTests
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@DirtiesContext(classMode = BEFORE_CLASS)
@Sql(scripts = {"recreate-schema.sql", "data-add-catbert.sql"}, executionPhase = BEFORE_TEST_CLASS)
class SqlScriptExecutionPhaseNestedTests extends AbstractTransactionalTests {

	@Test
	void outerClassLevelScriptsHaveBeenRun() {
		assertUsers("Catbert");
	}

	/**
	 * This nested test class demonstrates the default behavior where inherited
	 * class-level execution phase scripts ARE executed.
	 */
	@Nested
	class DefaultBehaviorNestedTests {

		@Test
		void inheritedClassLevelScriptsAreExecuted() {
			// The outer class's BEFORE_TEST_CLASS scripts are inherited and executed
			assertUsers("Catbert");
		}
	}

	/**
	 * This nested test class demonstrates the NEW behavior using
	 * {@link MergeMode#OVERRIDE_AND_EXCLUDE_INHERITED_EXECUTION_PHASE_SCRIPTS}
	 * where inherited class-level execution phase scripts are NOT executed.
	 */
	@Nested
	@SqlMergeMode(MergeMode.OVERRIDE_AND_EXCLUDE_INHERITED_EXECUTION_PHASE_SCRIPTS)
	class ExcludeInheritedExecutionPhaseScriptsNestedTests {

		@Test
		void inheritedClassLevelExecutionPhaseScriptsAreExcluded() {
			// The outer class's BEFORE_TEST_CLASS scripts are excluded
			// So the database should be empty (no users)
			assertUsers(); // Expects no users
		}

		@Test
		@Sql("data-add-dogbert.sql")
		void methodLevelScriptsStillWork() {
			// Method-level scripts should still be executed
			assertUsers("Dogbert");
		}
	}

	/**
	 * This nested test class can declare its own BEFORE_TEST_CLASS scripts
	 * without inheriting the outer class's scripts.
	 */
	@Nested
	@SqlMergeMode(MergeMode.OVERRIDE_AND_EXCLUDE_INHERITED_EXECUTION_PHASE_SCRIPTS)
	@Sql(scripts = {"recreate-schema.sql", "data-add-dogbert.sql"}, executionPhase = BEFORE_TEST_CLASS)
	class OwnExecutionPhaseScriptsNestedTests {

		@Test
		void ownClassLevelScriptsAreExecuted() {
			// Only this nested class's BEFORE_TEST_CLASS scripts run (Dogbert)
			// The outer class's scripts (Catbert) are excluded
			assertUsers("Dogbert");
		}
	}

}
