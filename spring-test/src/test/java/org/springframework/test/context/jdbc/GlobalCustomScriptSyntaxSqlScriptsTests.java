/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Modified copy of {@link CustomScriptSyntaxSqlScriptsTests} with
 * {@link SqlConfig @SqlConfig} defined at the class level.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
@SqlConfig(commentPrefixes = { "`", "%%" }, blockCommentStartDelimiter = "#$", blockCommentEndDelimiter = "$#", separator = "@@")
class GlobalCustomScriptSyntaxSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	@Sql(scripts = "schema.sql", config = @SqlConfig(separator = ";"))
	@Sql("data-add-users-with-custom-script-syntax.sql")
	void methodLevelScripts() {
		assertNumUsers(3);
	}

}
