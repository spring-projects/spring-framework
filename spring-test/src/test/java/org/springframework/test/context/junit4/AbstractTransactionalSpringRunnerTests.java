/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
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
@SuppressWarnings("deprecation")
@ContextConfiguration("transactionalTests-context.xml")
public abstract class AbstractTransactionalSpringRunnerTests {

	protected static final String BOB = "bob";
	protected static final String JANE = "jane";
	protected static final String SUE = "sue";
	protected static final String LUKE = "luke";
	protected static final String LEIA = "leia";
	protected static final String YODA = "yoda";


	protected static int clearPersonTable(SimpleJdbcTemplate simpleJdbcTemplate) {
		return simpleJdbcTemplate.update("DELETE FROM person");
	}

	protected static void createPersonTable(SimpleJdbcTemplate simpleJdbcTemplate) {
		try {
			simpleJdbcTemplate.update("CREATE TABLE person (name VARCHAR(20) NOT NULL, PRIMARY KEY(name))");
		}
		catch (BadSqlGrammarException bsge) {
			// ignore
		}
	}

	protected static int countRowsInPersonTable(SimpleJdbcTemplate simpleJdbcTemplate) {
		return simpleJdbcTemplate.queryForInt("SELECT COUNT(0) FROM person");
	}

	protected static int addPerson(SimpleJdbcTemplate simpleJdbcTemplate, String name) {
		return simpleJdbcTemplate.update("INSERT INTO person VALUES(?)", name);
	}

	protected static int deletePerson(SimpleJdbcTemplate simpleJdbcTemplate, String name) {
		return simpleJdbcTemplate.update("DELETE FROM person WHERE name=?", name);
	}

}
