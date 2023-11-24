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

package org.springframework.test.context.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.InfrastructureProxy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Transactional integration tests for {@link Sql @Sql} support when the
 * {@link DataSource} is wrapped in a proxy that implements
 * {@link InfrastructureProxy}.
 *
 * @author Sam Brannen
 * @since 5.3.4
 */
@SpringJUnitConfig
@DirtiesContext
class InfrastructureProxyTransactionalSqlScriptsTests extends AbstractTransactionalTests {

	@BeforeEach
	void preconditions(@Autowired DataSource dataSource, @Autowired DataSourceTransactionManager transactionManager) {
		assertThat(dataSource).isNotEqualTo(transactionManager.getDataSource());
		assertThat(transactionManager.getDataSource()).isNotEqualTo(dataSource);
		assertThat(transactionManager.getDataSource()).isInstanceOf(InfrastructureProxy.class);
	}

	@Test
	@Sql({ "schema.sql", "data.sql", "data-add-dogbert.sql" })
	void methodLevelScripts() {
		assertNumUsers(2);
	}


	@Configuration
	static class DatabaseConfig {

		@Bean
		JdbcTemplate jdbcTemplate(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(wrapDataSource(dataSource));
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder()//
					.generateUniqueName(true)
					.build();
		}

	}


	private static DataSource wrapDataSource(DataSource dataSource) {
		return (DataSource) Proxy.newProxyInstance(
			InfrastructureProxyTransactionalSqlScriptsTests.class.getClassLoader(),
			new Class<?>[] { DataSource.class, InfrastructureProxy.class },
			new DataSourceInvocationHandler(dataSource));
	}


	private static class DataSourceInvocationHandler implements InvocationHandler {

		private final DataSource dataSource;


		DataSourceInvocationHandler(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return switch (method.getName()) {
				case "equals" -> (proxy == args[0]);
				case "hashCode" -> System.identityHashCode(proxy);
				case "getWrappedObject" -> this.dataSource;
				default -> {
					try {
						yield method.invoke(this.dataSource, args);
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					}
				}
			};
		}
	}

}
