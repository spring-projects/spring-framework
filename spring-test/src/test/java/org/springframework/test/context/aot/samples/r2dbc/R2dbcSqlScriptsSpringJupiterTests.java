/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.aot.samples.r2dbc;

import io.r2dbc.spi.ConnectionFactory;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.reactive.EmptyReactiveDatabaseConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.context.jdbc.SqlMergeMode.MergeMode.MERGE;
import static org.springframework.test.r2dbc.R2dbcTestUtils.countRowsInTable;

/**
 * @author jonghoon park
 * @since 7.0
 */
@SpringJUnitConfig(EmptyReactiveDatabaseConfig.class)
@SqlMergeMode(MERGE)
@Sql("/org/springframework/test/context/r2dbc/schema.sql")
@DirtiesContext
@TestPropertySource(properties = "test.engine = jupiter")
public class R2dbcSqlScriptsSpringJupiterTests {

	@Test
	@Sql // default script --> org/springframework/test/context/aot/samples/r2dbc/R2dbcSqlScriptsSpringJupiterTests.test.sql
	void test(@Autowired ConnectionFactory connectionFactory) {
		StepVerifier.create(countRowsInTable(connectionFactory, "users"))
				.assertNext(count -> assertThat(count).isEqualTo(1))
				.verifyComplete();
	}

}
