/*
 * Copyright 2002-2023 the original author or authors.
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
