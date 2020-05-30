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

package org.springframework.test.context.junit4;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Extension of {@link DefaultRollbackFalseRollbackAnnotationTransactionalTests}
 * which tests method-level <em>rollback override</em> behavior via the
 * {@link Rollback @Rollback} annotation.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see Rollback
 */
public class RollbackOverrideDefaultRollbackFalseTransactionalTests
		extends DefaultRollbackFalseRollbackAnnotationTransactionalTests {

	private static int originalNumRows;

	private static JdbcTemplate jdbcTemplate;


	@Autowired
	@Override
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Before
	@Override
	public void verifyInitialTestData() {
		originalNumRows = clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
	}

	@Test
	@Rollback
	@Override
	public void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertThat(deletePerson(jdbcTemplate, BOB)).as("Deleting bob").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(2);
	}

	@AfterClass
	public static void verifyFinalTestData() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(originalNumRows);
	}

}
