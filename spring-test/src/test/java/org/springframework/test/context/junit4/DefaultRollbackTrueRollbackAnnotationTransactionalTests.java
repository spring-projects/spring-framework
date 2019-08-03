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
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * Integration test which verifies proper transactional behavior when the default
 * rollback flag is explicitly set to {@code true} via {@link Rollback @Rollback}.
 *
 * <p>Also tests configuration of the transaction manager qualifier configured
 * via {@link Transactional @Transactional}.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Rollback
 * @see Transactional#transactionManager
 * @see DefaultRollbackTrueTransactionalTests
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = EmbeddedPersonDatabaseTestsConfig.class, inheritLocations = false)
@Transactional("txMgr")
@Rollback(true)
public class DefaultRollbackTrueRollbackAnnotationTransactionalTests extends AbstractTransactionalSpringRunnerTests {

	private static int originalNumRows;

	private static JdbcTemplate jdbcTemplate;


	@Autowired
	public void setDataSource(DataSource dataSource) {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}


	@Before
	public void verifyInitialTestData() {
		originalNumRows = clearPersonTable(jdbcTemplate);
		assertThat(addPerson(jdbcTemplate, BOB)).as("Adding bob").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the initial number of rows in the person table.").isEqualTo(1);
	}

	@Test(timeout = 1000)
	public void modifyTestDataWithinTransaction() {
		assertThatTransaction().isActive();
		assertThat(addPerson(jdbcTemplate, JANE)).as("Adding jane").isEqualTo(1);
		assertThat(addPerson(jdbcTemplate, SUE)).as("Adding sue").isEqualTo(1);
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the number of rows in the person table within a transaction.").isEqualTo(3);
	}

	@AfterClass
	public static void verifyFinalTestData() {
		assertThat(countRowsInPersonTable(jdbcTemplate)).as("Verifying the final number of rows in the person table after all tests.").isEqualTo(originalNumRows);
	}

}
