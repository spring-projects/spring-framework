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

package org.springframework.test.context.aot.samples.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.EmptyDatabaseConfig;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.jdbc.JdbcTestUtils.countRowsInTable;

/**
 * @author Sam Brannen
 * @since 6.0
 */
@SpringJUnitConfig(EmptyDatabaseConfig.class)
@Transactional
@SqlMergeMode(MERGE)
@Sql("/org/springframework/test/context/jdbc/schema.sql")
@DirtiesContext
@TestPropertySource(properties = "test.engine = jupiter")
public class SqlScriptsSpringJupiterTests {

	@Test
	@Sql // default script --> org/springframework/test/context/aot/samples/jdbc/SqlScriptsSpringJupiterTests.test.sql
	void test(@Autowired JdbcTemplate jdbcTemplate) {
		assertThat(countRowsInTable(jdbcTemplate, "user")).isEqualTo(1);
	}

}
