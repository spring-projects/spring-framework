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
