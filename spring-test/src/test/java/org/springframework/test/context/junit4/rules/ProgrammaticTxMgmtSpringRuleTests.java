/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * This class is a copy of
 * {@link org.springframework.test.context.transaction.programmatic.ProgrammaticTxMgmtTests}
 * that has been modified to use {@link SpringClassRule} and {@link SpringMethodRule}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
@RunWith(JUnit4.class)
@ContextConfiguration
@Transactional
public class ProgrammaticTxMgmtSpringRuleTests {

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	public TestName testName = new TestName();

	String sqlScriptEncoding;
	JdbcTemplate jdbcTemplate;

	@Autowired
	ApplicationContext applicationContext;

	@Autowired
	void setDataSource(DataSource dataSource) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@BeforeTransaction
	void beforeTransaction() {
		deleteFromTables("user");
		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data.sql", false);
	}

	@AfterTransaction
	void afterTransaction() {
		String method = this.testName.getMethodName();
		switch (method) {
			case "commitTxAndStartNewTx", "commitTxButDoNotStartNewTx" -> assertUsers("Dogbert");
			case "rollbackTxAndStartNewTx", "rollbackTxButDoNotStartNewTx", "startTxWithExistingTransaction" ->
					assertUsers("Dilbert");
			case "rollbackTxAndStartNewTxWithDefaultCommitSemantics" -> assertUsers("Dilbert", "Dogbert");
			default -> fail("missing 'after transaction' assertion for test method: " + method);
		}
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void isActiveWithNonExistentTransactionContext() {
		assertThat(TestTransaction.isActive()).isFalse();
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void flagForRollbackWithNonExistentTransactionContext() {
		assertThatIllegalStateException().isThrownBy(TestTransaction::flagForRollback);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void flagForCommitWithNonExistentTransactionContext() {
		assertThatIllegalStateException().isThrownBy(TestTransaction::flagForCommit);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void isFlaggedForRollbackWithNonExistentTransactionContext() {
		assertThatIllegalStateException().isThrownBy(TestTransaction::isFlaggedForRollback);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void startTxWithNonExistentTransactionContext() {
		assertThatIllegalStateException().isThrownBy(TestTransaction::start);
	}

	@Test
	public void startTxWithExistingTransaction() {
		assertThatIllegalStateException().isThrownBy(TestTransaction::start);
	}

	@Test
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void endTxWithNonExistentTransactionContext() {
		assertThatIllegalStateException().isThrownBy(TestTransaction::end);
	}

	@Test
	public void commitTxAndStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();
		assertThatTransaction().isNotActive();
		assertThat(TestTransaction.isActive()).isFalse();
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");

		TestTransaction.start();
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
	}

	@Test
	public void commitTxButDoNotStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Commit
		TestTransaction.flagForCommit();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dogbert");
	}

	@Test
	public void rollbackTxAndStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers("Dilbert");

		// Start new transaction with default rollback semantics
		TestTransaction.start();
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		assertThat(TestTransaction.isActive()).isTrue();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	@Test
	public void rollbackTxButDoNotStartNewTx() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback (automatically)
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers("Dilbert");
	}

	@Test
	@Commit
	public void rollbackTxAndStartNewTxWithDefaultCommitSemantics() {
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isActive()).isTrue();
		assertUsers("Dilbert");
		deleteFromTables("user");
		assertUsers();

		// Rollback
		TestTransaction.flagForRollback();
		assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
		TestTransaction.end();
		assertThat(TestTransaction.isActive()).isFalse();
		assertThatTransaction().isNotActive();
		assertUsers("Dilbert");

		// Start new transaction with default commit semantics
		TestTransaction.start();
		assertThatTransaction().isActive();
		assertThat(TestTransaction.isFlaggedForRollback()).isFalse();
		assertThat(TestTransaction.isActive()).isTrue();

		executeSqlScript("classpath:/org/springframework/test/context/jdbc/data-add-dogbert.sql", false);
		assertUsers("Dilbert", "Dogbert");
	}

	protected int deleteFromTables(String... names) {
		return JdbcTestUtils.deleteFromTables(this.jdbcTemplate, names);
	}

	protected void executeSqlScript(String sqlResourcePath, boolean continueOnError) throws DataAccessException {
		Resource resource = this.applicationContext.getResource(sqlResourcePath);
		new ResourceDatabasePopulator(continueOnError, false, this.sqlScriptEncoding, resource).execute(jdbcTemplate.getDataSource());
	}

	private void assertUsers(String... users) {
		List<String> expected = Arrays.asList(users);
		Collections.sort(expected);
		List<String> actual = jdbcTemplate.queryForList("select name from user", String.class);
		Collections.sort(actual);
		assertThat(actual).as("Users in database;").isEqualTo(expected);
	}


	@Configuration
	static class Config {

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
			.generateUniqueName(true)//
			.addScript("classpath:/org/springframework/test/context/jdbc/schema.sql") //
			.build();
		}
	}

}
