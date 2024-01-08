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

package org.springframework.jdbc.support;

import java.sql.Types;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdbcUtils}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Ben Blinebury
 */
class JdbcUtilsTests {

	@Test
	void commonDatabaseName() {
		assertThat(JdbcUtils.commonDatabaseName("Oracle")).isEqualTo("Oracle");
		assertThat(JdbcUtils.commonDatabaseName("DB2-for-Spring")).isEqualTo("DB2");
		assertThat(JdbcUtils.commonDatabaseName("Sybase SQL Server")).isEqualTo("Sybase");
		assertThat(JdbcUtils.commonDatabaseName("Adaptive Server Enterprise")).isEqualTo("Sybase");
		assertThat(JdbcUtils.commonDatabaseName("MySQL")).isEqualTo("MySQL");
		assertThat(JdbcUtils.commonDatabaseName("MariaDB")).isEqualTo("MariaDB");
	}

	@Test
	void resolveTypeName() {
		assertThat(JdbcUtils.resolveTypeName(Types.VARCHAR)).isEqualTo("VARCHAR");
		assertThat(JdbcUtils.resolveTypeName(Types.NUMERIC)).isEqualTo("NUMERIC");
		assertThat(JdbcUtils.resolveTypeName(Types.INTEGER)).isEqualTo("INTEGER");
		assertThat(JdbcUtils.resolveTypeName(JdbcUtils.TYPE_UNKNOWN)).isNull();
	}

	@Test
	void convertUnderscoreNameToPropertyName() {
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME")).isEqualTo("myName");
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME")).isEqualTo("yourName");
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("a_name")).isEqualTo("AName");
		assertThat(JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name")).isEqualTo("someoneElsesName");
	}

}
