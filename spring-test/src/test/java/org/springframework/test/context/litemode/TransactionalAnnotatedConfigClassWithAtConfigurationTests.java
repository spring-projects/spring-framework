/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.litemode;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.testfixture.beans.Employee;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concrete implementation of {@link AbstractTransactionalAnnotatedConfigClassTests}
 * that uses a true {@link Configuration @Configuration class}.
 *
 * @author Sam Brannen
 * @since 3.2
 * @see TransactionalAnnotatedConfigClassesWithoutAtConfigurationTests
 */
@ContextConfiguration
class TransactionalAnnotatedConfigClassWithAtConfigurationTests extends
		AbstractTransactionalAnnotatedConfigClassTests {

	/**
	 * This is <b>intentionally</b> annotated with {@code @Configuration}.
	 *
	 * <p>Consequently, this class contains standard singleton bean methods
	 * instead of <i>annotated factory bean methods</i>.
	 */
	@Configuration
	static class Config {

		@Bean
		Employee employee() {
			Employee employee = new Employee();
			employee.setName("John Smith");
			employee.setAge(42);
			employee.setCompany("Acme Widgets, Inc.");
			return employee;
		}

		@Bean
		PlatformTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
					.addScript("classpath:/org/springframework/test/jdbc/schema.sql")//
					// Ensure that this in-memory database is only used by this class:
					.setName(getClass().getName())//
					.build();
		}

	}


	@BeforeEach
	void compareDataSources() {
		// NOTE: the two DataSource instances ARE the same!
		assertThat(dataSourceViaInjection).isSameAs(dataSourceFromTxManager);
	}

}
