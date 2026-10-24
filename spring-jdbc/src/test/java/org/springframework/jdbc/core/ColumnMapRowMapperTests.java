/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jdbc.core;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ColumnMapRowMapper}.
 *
 * @author Sharang Gupta
 * @since 7.1
 */
class ColumnMapRowMapperTests {

	private final ColumnMapRowMapper mapper = new ColumnMapRowMapper();


	@Test
	void columnKeysAreCaseInsensitive() throws SQLException {
		Map<String, Object> row = this.mapper.mapRow(resultSetWithColumn("ID", 42), 0);

		assertThat(row).containsEntry("ID", 42);
		assertThat(row.get("id")).isEqualTo(42);
		assertThat(row.get("Id")).isEqualTo(42);
	}

	@Test  // "ID" vs "id" must keep matching when the default Locale folds 'I' to a dotless i
	void columnKeysAreCaseInsensitiveRegardlessOfDefaultLocale() throws SQLException {
		Locale defaultLocale = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr-TR"));
		try {
			Map<String, Object> row = this.mapper.mapRow(resultSetWithColumn("ID", 42), 0);

			assertThat(row.get("id")).as("lower-case lookup under Turkish default Locale").isEqualTo(42);
			assertThat(row.containsKey("id")).isTrue();
		}
		finally {
			Locale.setDefault(defaultLocale);
		}
	}


	private static ResultSet resultSetWithColumn(String label, Object value) throws SQLException {
		ResultSetMetaData metaData = mock();
		when(metaData.getColumnCount()).thenReturn(1);
		when(metaData.getColumnLabel(1)).thenReturn(label);
		when(metaData.getColumnName(1)).thenReturn(label);
		ResultSet resultSet = mock();
		when(resultSet.getMetaData()).thenReturn(metaData);
		when(resultSet.getObject(1)).thenReturn(value);
		return resultSet;
	}

}
