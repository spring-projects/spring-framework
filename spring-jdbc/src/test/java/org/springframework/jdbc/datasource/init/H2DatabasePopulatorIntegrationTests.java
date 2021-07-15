/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0.3
 */
class H2DatabasePopulatorIntegrationTests extends AbstractDatabasePopulatorTests {

	@Override
	protected EmbeddedDatabaseType getEmbeddedDatabaseType() {
		return EmbeddedDatabaseType.H2;
	}

	/**
	 * https://jira.spring.io/browse/SPR-15896
	 *
	 * @since 5.0
	 */
	@Test
	void scriptWithH2Alias() throws Exception {
		databasePopulator.addScript(usersSchema());
		databasePopulator.addScript(resource("db-test-data-h2-alias.sql"));
		// Set statement separator to double newline so that ";" is not
		// considered a statement separator within the source code of the
		// aliased function 'REVERSE'.
		databasePopulator.setSeparator("\n\n");
		DatabasePopulatorUtils.execute(databasePopulator, db);
		String sql = "select REVERSE(first_name) from users where last_name='Brannen'";
		assertThat(jdbcTemplate.queryForObject(sql, String.class)).isEqualTo("maS");
	}

}
