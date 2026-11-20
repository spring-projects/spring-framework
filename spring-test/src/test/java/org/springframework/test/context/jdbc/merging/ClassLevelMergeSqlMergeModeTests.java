/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * {@link SqlMergeMode.MergeMode#MERGE MERGE} mode.
 *
 * @author Sam Brannen
 * @author Dmitry Semukhin
 * @since 5.2
 */
@Sql({ "../recreate-schema.sql", "../data-add-catbert.sql" })
@SqlMergeMode(MERGE)
class ClassLevelMergeSqlMergeModeTests extends AbstractSqlMergeModeTests {

	@Test
	void classLevelScripts() {
		assertUsers("Catbert");
	}

	@Test
	@Sql("../data-add-dogbert.sql")
	void merged() {
		assertUsers("Catbert", "Dogbert");
	}

	@Test
	@Sql({ "../recreate-schema.sql", "../data.sql", "../data-add-dogbert.sql", "../data-add-catbert.sql" })
	@SqlMergeMode(OVERRIDE)
	void overridden() {
		assertUsers("Dilbert", "Dogbert", "Catbert");
	}

}
