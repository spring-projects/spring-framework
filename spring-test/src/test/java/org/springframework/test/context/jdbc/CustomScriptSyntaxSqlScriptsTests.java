/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests that verify support for custom SQL script syntax
 * configured via {@link SqlConfig @SqlConfig}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
class CustomScriptSyntaxSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	@Sql("schema.sql")
	@Sql(scripts = "data-add-users-with-custom-script-syntax.sql",//
	config = @SqlConfig(commentPrefixes = { "`", "%%" }, blockCommentStartDelimiter = "#$", blockCommentEndDelimiter = "$#", separator = "@@"))
	void methodLevelScripts() {
		assertNumUsers(3);
	}

}
