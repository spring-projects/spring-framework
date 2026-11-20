/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Subclass of {@link RepeatableSqlAnnotationSqlScriptsParentTests} which verifies
 * that {@link Repeatable} {@link Sql @Sql} annotations are not
 * {@linkplain Inherited @Inherited} from a superclass if the subclass has local
 * {@code @Sql} declarations.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@Sql("schema.sql")
@Sql("data-add-catbert.sql")
class RepeatableSqlAnnotationSqlScriptsChildTests extends RepeatableSqlAnnotationSqlScriptsParentTests {

	@Test
	@Order(1)
	@Override
	void classLevelScripts() {
		// Should not find Dilbert, since local @Sql declarations shadow @Sql
		// declarations on a superclass. This is due to the fact that we use
		// "get" semantics instead of "find" semantics when searching for @Sql.
		assertUsers("Catbert");
	}

}
