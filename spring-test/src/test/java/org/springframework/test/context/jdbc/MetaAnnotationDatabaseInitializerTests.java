/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;

/**
 * Integration tests that verify support for using
 * {@link DatabaseInitializer @DatabaseInitializer} and
 * {@link DatabaseInitializers @DatabaseInitializers} as a meta-annotations.
 *
 * @author Sam Brannen
 * @since 4.1
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
public class MetaAnnotationDatabaseInitializerTests extends AbstractTransactionalJUnit4SpringContextTests {

	@Test
	@MetaDbInitializer
	public void metaDatabaseInitializer() {
		assertNumUsers(1);
	}

	@Test
	@MetaDbInitializers
	public void metaDatabaseInitializers() {
		assertNumUsers(1);
	}

	protected void assertNumUsers(int expected) {
		assertEquals("Number of rows in the 'user' table.", expected, countRowsInTable("user"));
	}


	@DatabaseInitializer({ "drop-schema.sql", "schema.sql", "data.sql" })
	@Retention(RUNTIME)
	@Target(METHOD)
	static @interface MetaDbInitializer {
	}

	@DatabaseInitializers({//
	@DatabaseInitializer("drop-schema.sql"),//
		@DatabaseInitializer("schema.sql"),//
		@DatabaseInitializer("data.sql") //
	})
	@Retention(RUNTIME)
	@Target(METHOD)
	static @interface MetaDbInitializers {
	}

}
