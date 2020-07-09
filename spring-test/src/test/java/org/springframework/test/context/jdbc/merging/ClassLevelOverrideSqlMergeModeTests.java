/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.context.jdbc.merging;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;

import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.OVERRIDE;

/**
 * Transactional integration tests that verify proper merging and overriding support
 * for class-level and method-level {@link Sql @Sql} declarations when
 * {@link SqlMergeMode @SqlMergeMode} is declared at the class level with
 * {@link SqlMergeMode.MergeMode#OVERRIDE OVERRIDE} mode.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 5.2
 */
@Sql({ "../recreate-schema.sql", "../data-add-catbert.sql" })
@SqlMergeMode(OVERRIDE)
class ClassLevelOverrideSqlMergeModeTests extends AbstractSqlMergeModeTests {

	@Test
	void classLevelScripts() {
		assertUsers("Catbert");
	}

	@Test
	@Sql("../data-add-dogbert.sql")
	@SqlMergeMode(MERGE)
	void merged() {
		assertUsers("Catbert", "Dogbert");
	}

	@Test
	@Sql({ "../recreate-schema.sql", "../data.sql", "../data-add-dogbert.sql", "../data-add-catbert.sql" })
	void overridden() {
		assertUsers("Dilbert", "Dogbert", "Catbert");
	}

}
