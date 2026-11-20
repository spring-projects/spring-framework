/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import java.lang.annotation.Retention;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

/**
 * Integration tests that verify support for using {@link Sql @Sql} as a
 * merged, composed annotation.
 *
 * @author Sam Brannen
 * @since 4.3
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@DirtiesContext
class ComposedAnnotationSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	@ComposedSql(
		scripts = { "drop-schema.sql", "schema.sql" },
		statements = "INSERT INTO user VALUES('Dilbert')",
		executionPhase = BEFORE_TEST_METHOD
	)
	void composedSqlAnnotation() {
		assertNumUsers(1);
	}


	@Sql
	@Retention(RUNTIME)
	@interface ComposedSql {

		@AliasFor(annotation = Sql.class)
		String[] value() default {};

		@AliasFor(annotation = Sql.class)
		String[] scripts() default {};

		@AliasFor(annotation = Sql.class)
		String[] statements() default {};

		@AliasFor(annotation = Sql.class)
		ExecutionPhase executionPhase();
	}

}
