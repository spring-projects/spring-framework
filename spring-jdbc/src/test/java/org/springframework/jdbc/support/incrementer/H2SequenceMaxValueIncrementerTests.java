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

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.JdbcTemplate;
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

	@Test
	void incrementsSequence() {
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

}
