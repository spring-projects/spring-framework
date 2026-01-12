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
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.jdbc.AbstractTransactionalTests;
import org.springframework.test.context.jdbc.PopulatedSchemaDatabaseConfig;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.jdbc.SqlMergeMode.MergeMode;
import org.springframework.test.context.jdbc.merging.AbstractSqlMergeModeTests;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

/**
 * Integration tests that verify support for {@link Nested @Nested} test classes in
 * conjunction with the {@link SpringExtension}, {@link Sql @Sql}, and
 * {@link Transactional @Transactional} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.1.3
 */
@SpringJUnitConfig(PopulatedSchemaDatabaseConfig.class)
@TestInstance(Lifecycle.PER_CLASS)
class SqlScriptNestedTests extends AbstractTransactionalTests {

	@BeforeTransaction
	@AfterTransaction
	void checkInitialDatabaseState() {
		assertNumUsers(0);
	}

	@Test
	@Sql("/org/springframework/test/context/jdbc/data.sql")
	void sqlScripts() {
		assertUsers("Dilbert");
	}

	@Nested
	class NestedTests {

		@BeforeTransaction
		@AfterTransaction
		void checkInitialDatabaseState() {
			assertNumUsers(0);
		}

		@Test
		@Sql("/org/springframework/test/context/jdbc/data.sql")
		void nestedSqlScripts() {
			assertUsers("Dilbert");
		}
	}

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@Sql({
		"/org/springframework/test/context/jdbc/recreate-schema.sql",
		"/org/springframework/test/context/jdbc/data-add-catbert.sql"
	})
	class NestedSqlMergeModeTests extends AbstractSqlMergeModeTests {

		@Nested
		@NestedTestConfiguration(EnclosingConfiguration.INHERIT)
		@SqlMergeMode(MergeMode.MERGE)
		class NestedClassLevelMergeSqlMergeModeTests {

			@Test
			void classLevelScripts() {
				assertUsers("Catbert");
			}

			@Test
			@Sql("/org/springframework/test/context/jdbc/data-add-dogbert.sql")
			void merged() {
				assertUsers("Catbert", "Dogbert");
			}

			@Test
			@Sql({
				"/org/springframework/test/context/jdbc/recreate-schema.sql",
				"/org/springframework/test/context/jdbc/data.sql",
				"/org/springframework/test/context/jdbc/data-add-dogbert.sql",
				"/org/springframework/test/context/jdbc/data-add-catbert.sql"
			})
			@SqlMergeMode(MergeMode.OVERRIDE)
			void overridden() {
				assertUsers("Dilbert", "Dogbert", "Catbert");
			}
		}

		@Nested
		@NestedTestConfiguration(EnclosingConfiguration.INHERIT)
		@SqlMergeMode(OVERRIDE)
		class ClassLevelOverrideSqlMergeModeTests {

			@Test
			void classLevelScripts() {
				assertUsers("Catbert");
			}

			@Test
			@Sql("/org/springframework/test/context/jdbc/data-add-dogbert.sql")
			@SqlMergeMode(MERGE)
			void merged() {
				assertUsers("Catbert", "Dogbert");
			}

			@Test
			@Sql({
				"/org/springframework/test/context/jdbc/recreate-schema.sql",
				"/org/springframework/test/context/jdbc/data.sql",
				"/org/springframework/test/context/jdbc/data-add-dogbert.sql",
				"/org/springframework/test/context/jdbc/data-add-catbert.sql"
			})
			void overridden() {
				assertUsers("Dilbert", "Dogbert", "Catbert");
			}
		}

	}

}
