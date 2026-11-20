/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
