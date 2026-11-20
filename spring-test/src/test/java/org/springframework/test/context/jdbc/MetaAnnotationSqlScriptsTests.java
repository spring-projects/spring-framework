/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.jdbc;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.junit.jupiter.api.Test;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Integration tests that verify support for using {@link Sql @Sql} and
 * {@link SqlGroup @SqlGroup} as meta-annotations.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
class MetaAnnotationSqlScriptsTests extends AbstractTransactionalTests {

	@Test
	@MetaSql
	void metaSqlAnnotation() {
		assertNumUsers(1);
	}

	@Test
	@MetaSqlGroup
	void metaSqlGroupAnnotation() {
		assertNumUsers(1);
	}


	@Sql({ "drop-schema.sql", "schema.sql", "data.sql" })
	@Retention(RUNTIME)
	@Target(METHOD)
	@interface MetaSql {
	}

	@SqlGroup({ @Sql("drop-schema.sql"), @Sql("schema.sql"), @Sql("data.sql") })
	@Retention(RUNTIME)
	@Target(METHOD)
	@interface MetaSqlGroup {
	}

}
