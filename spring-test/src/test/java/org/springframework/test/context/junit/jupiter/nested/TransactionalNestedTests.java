/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.test.context.junit.jupiter.nested;

import javax.sql.DataSource;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.Commit;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.INHERIT;
import static org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration.OVERRIDE;
import static org.springframework.test.transaction.TransactionAssert.assertThatTransaction;
import static org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED;

/**
 * Integration tests that verify support for {@code @Nested} test classes using
 * {@link Transactional @Transactional} in conjunction with the
 * {@link SpringExtension} in a JUnit Jupiter environment.
 *
 * @author Sam Brannen
 * @since 5.3
 */
@SpringJUnitConfig
@Transactional
@Commit
@NestedTestConfiguration(OVERRIDE) // since INHERIT is now the global default
class TransactionalNestedTests {

	@Test
	void transactional(@Autowired DataSource dataSource) {
		assertThatTransaction().isActive();
		assertThat(dataSource).isNotNull();
		assertCommit();
	}


	@Nested
	@SpringJUnitConfig(Config.class)
	class ConfigOverriddenByDefaultTests {

		@Test
		void notTransactional(@Autowired DataSource dataSource) {
			assertThatTransaction().isNotActive();
			assertThat(dataSource).isNotNull();
		}
	}

	@Nested
	@NestedTestConfiguration(INHERIT)
	class InheritedConfigTests {

		@Test
		void transactional(@Autowired DataSource dataSource) {
			assertThatTransaction().isActive();
			assertThat(dataSource).isNotNull();
			assertCommit();
		}


		@Nested
		class DoubleNestedWithImplicitlyInheritedConfigTests {

			@Test
			void transactional(@Autowired DataSource dataSource) {
				assertThatTransaction().isActive();
				assertThat(dataSource).isNotNull();
				assertCommit();
			}


			@Nested
			@Rollback
			class TripleNestedWithImplicitlyInheritedConfigTests {

				@Test
				void transactional(@Autowired DataSource dataSource) {
					assertThatTransaction().isActive();
					assertThat(dataSource).isNotNull();
					assertRollback();
				}
			}
		}

		@Nested
		@NestedTestConfiguration(OVERRIDE)
		@SpringJUnitConfig(Config.class)
		@Transactional
		@Rollback
		class DoubleNestedWithOverriddenConfigTests {

			@Test
			void transactional(@Autowired DataSource dataSource) {
				assertThatTransaction().isActive();
				assertThat(dataSource).isNotNull();
				assertRollback();
			}


			@Nested
			@NestedTestConfiguration(INHERIT)
			@Commit
			class TripleNestedWithInheritedConfigTests {

				@Test
				void transactional(@Autowired DataSource dataSource) {
					assertThatTransaction().isActive();
					assertThat(dataSource).isNotNull();
					assertCommit();
				}
			}

			@Nested
			@NestedTestConfiguration(INHERIT)
			class TripleNestedWithInheritedConfigAndTestInterfaceTests implements TestInterface {

				@Test
				void notTransactional(@Autowired DataSource dataSource) {
					assertThatTransaction().isNotActive();
					assertThat(dataSource).isNotNull();
				}
			}
		}
	}


	private void assertCommit() {
		assertThat(TestTransaction.isFlaggedForRollback()).as("flagged for commit").isFalse();
	}

	private void assertRollback() {
		assertThat(TestTransaction.isFlaggedForRollback()).as("flagged for rollback").isTrue();
	}

	// -------------------------------------------------------------------------


	@Configuration
	@EnableTransactionManagement
	static class Config {

		@Bean
		TransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).build();
		}
	}

	@Transactional(propagation = NOT_SUPPORTED)
	interface TestInterface {
	}

}
