/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.junit.jupiter.transaction;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;

/**
 * JUnit Jupiter based integration tests which verify support for parameter
 * injection in {@link BeforeTransaction @BeforeTransaction} and
 * {@link AfterTransaction @AfterTransaction} lifecycle methods.
 *
 * @author Sam Brannen
 * @since 6.1
 */
@SpringJUnitConfig
class TransactionLifecycleMethodParameterInjectionTests {

	static boolean beforeTransactionInvoked = false;
	static boolean afterTransactionInvoked = false;


	@BeforeAll
	static void checkInitialFlagState() {
		assertThat(beforeTransactionInvoked).isFalse();
		assertThat(afterTransactionInvoked).isFalse();
	}

	@BeforeTransaction
	void beforeTransaction(TestInfo testInfo, ApplicationContext context, @Autowired DataSource dataSource) {
		assertThatTransaction().isNotActive();
		assertThat(testInfo).isNotNull();
		assertThat(context).isNotNull();
		assertThat(dataSource).isNotNull();
		beforeTransactionInvoked = true;
	}

	@Test
	@Transactional
	void transactionalTest(TestInfo testInfo, ApplicationContext context, @Autowired DataSource dataSource) {
		assertThatTransaction().isActive();
		assertThat(testInfo).isNotNull();
		assertThat(context).isNotNull();
		assertThat(dataSource).isNotNull();
		assertThat(beforeTransactionInvoked).isTrue();
		assertThat(afterTransactionInvoked).isFalse();
	}

	@AfterTransaction
	void afterTransaction(TestInfo testInfo, ApplicationContext context, @Autowired DataSource dataSource) {
		assertThatTransaction().isNotActive();
		assertThat(testInfo).isNotNull();
		assertThat(context).isNotNull();
		assertThat(dataSource).isNotNull();
		afterTransactionInvoked = true;
	}

	@AfterAll
	static void checkFinalFlagState() {
		assertThat(beforeTransactionInvoked).isTrue();
		assertThat(afterTransactionInvoked).isTrue();
	}


	@Configuration
	static class Config {

		@Bean
		DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}
	}

}
