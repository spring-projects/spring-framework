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

package org.springframework.test.context.jdbc.merging;

import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.EmptyDatabaseConfig;
import org.springframework.test.context.jdbc.SqlMergeMode;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for tests involving {@link SqlMergeMode @SqlMergeMode}.
 *
 * @author Sam Brannen
 * @since 5.2
 */
@ContextConfiguration(classes = EmptyDatabaseConfig.class)
@DirtiesContext
abstract class AbstractSqlMergeModeTests extends AbstractTransactionalJUnit4SpringContextTests {

	protected void assertUsers(String... expectedUsers) {
		List<String> actualUsers = super.jdbcTemplate.queryForList("select name from user", String.class);
		assertThat(actualUsers).containsExactlyInAnyOrder(expectedUsers);
	}

}
