/*
 * Copyright 2002-2024 the original author or authors.
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

import org.h2.engine.Mode.ModeEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

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

		assertIncrements(database);

		database.shutdown();
	}

	/**
	 * Tests that the incrementer works when using all supported H2 <em>compatibility modes</em>.
	 */
	@ParameterizedTest
	@EnumSource(ModeEnum.class)
	void incrementsSequenceWithExplicitH2CompatibilityMode(ModeEnum mode) {
		String connectionUrl = String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false;MODE=%s", UUID.randomUUID(), mode);
		DataSource dataSource = new SimpleDriverDataSource(new org.h2.Driver(), connectionUrl, "sa", "");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("CREATE SEQUENCE SEQ");

		assertIncrements(dataSource);

		jdbcTemplate.execute("SHUTDOWN");
	}

	private void assertIncrements(DataSource dataSource) {
		assertThat(new JdbcTemplate(dataSource).queryForObject("values next value for SEQ", int.class)).isEqualTo(1);

		H2SequenceMaxValueIncrementer incrementer = new H2SequenceMaxValueIncrementer(dataSource, "SEQ");
		assertThat(incrementer.nextIntValue()).isEqualTo(2);
		assertThat(incrementer.nextStringValue()).isEqualTo("3");
	}

}
