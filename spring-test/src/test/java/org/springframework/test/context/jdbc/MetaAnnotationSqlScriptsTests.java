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
