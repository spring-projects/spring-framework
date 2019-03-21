/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context.junit4.rules;

import java.util.concurrent.TimeUnit;

import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.MethodSorters;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.TransactionalSqlScriptsTests;

/**
 * This class is an extension of {@link TransactionalSqlScriptsTests}
 * that has been modified to use {@link SpringClassRule} and
 * {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
// Note: @FixMethodOrder is NOT @Inherited.
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
// Overriding @Sql declaration to reference scripts using relative path.
@Sql({ "../../jdbc/schema.sql", "../../jdbc/data.sql" })
public class TransactionalSqlScriptsSpringRuleTests extends TransactionalSqlScriptsTests {

	@ClassRule
	public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	public Timeout timeout = Timeout.builder().withTimeout(10, TimeUnit.SECONDS).build();


	/**
	 * Redeclared to ensure that {@code @FixMethodOrder} is properly applied.
	 */
	@Test
	@Override
	// test##_ prefix is required for @FixMethodOrder.
	public void test01_classLevelScripts() {
		assertNumUsers(1);
	}

	/**
	 * Overriding {@code @Sql} declaration to reference scripts using relative path.
	 */
	@Test
	@Sql({ "../../jdbc/drop-schema.sql", "../../jdbc/schema.sql", "../../jdbc/data.sql", "../../jdbc/data-add-dogbert.sql" })
	@Override
	// test##_ prefix is required for @FixMethodOrder.
	public void test02_methodLevelScripts() {
		assertNumUsers(2);
	}

}
