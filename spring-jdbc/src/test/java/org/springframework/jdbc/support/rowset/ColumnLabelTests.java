/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jdbc.support.rowset;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Maksym Navolochka
 */
public class ColumnLabelTests {

	@Test
	public void shouldBeCaseInsensitive() throws SQLException {
		ResultSetMetaData metaData = mock(ResultSetMetaData.class);
		// "select 0 as Name"
		given(metaData.getColumnCount()).willReturn(1);
		given(metaData.getColumnLabel(1)).willReturn("Name");
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.getMetaData()).willReturn(metaData);
		// com.sun.rowset.CachedRowSetImpl
		given(resultSet.findColumn("name")).willThrow(new SQLException("Invalid column name"));
		ResultSetWrappingSqlRowSet rowSet = new ResultSetWrappingSqlRowSet(resultSet);

		Assertions.assertEquals(
			1,
			rowSet.findColumn("name"),
			"Should be served from cache, no fallback to underlied ResultSet"
		);
	}
}
