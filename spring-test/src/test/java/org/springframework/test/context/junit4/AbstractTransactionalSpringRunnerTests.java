/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.context.junit4;

import org.junit.runner.RunWith;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstract base class for verifying support of Spring's {@link Transactional
 * &#64;Transactional} annotation.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ClassLevelTransactionalSpringRunnerTests
 * @see MethodLevelTransactionalSpringRunnerTests
 * @see Transactional
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("transactionalTests-context.xml")
public abstract class AbstractTransactionalSpringRunnerTests {

	protected static final String BOB = "bob";
	protected static final String JANE = "jane";
	protected static final String SUE = "sue";
	protected static final String LUKE = "luke";
	protected static final String LEIA = "leia";
	protected static final String YODA = "yoda";


	protected static int clearPersonTable(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.update("DELETE FROM person");
	}

	protected static int countRowsInPersonTable(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForObject("SELECT COUNT(0) FROM person", Integer.class);
	}

	protected static int addPerson(JdbcTemplate jdbcTemplate, String name) {
		return jdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	protected static int deletePerson(JdbcTemplate jdbcTemplate, String name) {
		return jdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

}
