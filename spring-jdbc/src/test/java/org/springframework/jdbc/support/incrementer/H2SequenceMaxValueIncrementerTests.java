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

package org.springframework.jdbc.support.incrementer;

import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link H2SequenceMaxValueIncrementer}.
 *
 * @author Henning Pöttker
 * @author Sam Brannen
 * @since 5.3.15
 */
class H2SequenceMaxValueIncrementerTests {

	/**
	 * Tests that the incrementer works when using the JDBC connection URL used
	 * in the {@code H2EmbeddedDatabaseConfigurer} which is used transparently
	 * when using Spring's {@link EmbeddedDatabaseBuilder}.
	 *
	 * <p>In other words, this tests compatibility with the default H2
	 * <em>compatibility mode</em>.
	 */
	@Test
	void incrementsSequenceUsingH2EmbeddedDatabaseConfigurer() {
		EmbeddedDatabase database = new EmbeddedDatabaseBuilder()
				.setType(EmbeddedDatabaseType.H2)
				.generateUniqueName(true)
				.addScript("classpath:/org/springframework/jdbc/support/incrementer/schema.sql")
				.build();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(database);
		assertThat(jdbcTemplate.queryForObject("values next value for SEQ", int.class)).isEqualTo(1);

		H2SequenceMaxValueIncrementer incrementer = new H2SequenceMaxValueIncrementer(database, "SEQ");
		assertThat(incrementer.nextIntValue()).isEqualTo(2);
		assertThat(incrementer.nextStringValue()).isEqualTo("3");

		database.shutdown();
	}

	/**
	 * Tests that the incrementer works when using all supported H2 <em>compatibility modes</em>.
	 *
	 * <p>The following modes are only supported with H2 2.x or higher: STRICT, LEGACY, MariaDB
	 */
	@ParameterizedTest
	@ValueSource(strings = { "DB2", "Derby", "HSQLDB", "MSSQLServer", "MySQL", "Oracle", "PostgreSQL" })
	void incrementsSequenceWithExplicitH2CompatibilityMode(String compatibilityMode) {
		String connectionUrl = String.format("jdbc:h2:mem:%s;MODE=%s", UUID.randomUUID().toString(), compatibilityMode);
		DataSource dataSource = new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

		transactionTemplate.executeWithoutResult(status -> {
			jdbcTemplate.execute("CREATE SEQUENCE SEQ");
			assertThat(jdbcTemplate.queryForObject("values next value for SEQ", int.class)).isEqualTo(1);

			H2SequenceMaxValueIncrementer incrementer = new H2SequenceMaxValueIncrementer(dataSource, "SEQ");
			assertThat(incrementer.nextIntValue()).isEqualTo(2);
			assertThat(incrementer.nextStringValue()).isEqualTo("3");
		});

		jdbcTemplate.execute("SHUTDOWN");
	}

}
