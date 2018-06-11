/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map;

import org.junit.Test;

import static org.mockito.BDDMockito.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link ColumnMapRowMapper}.
 *
 * @author Philippe Marschall
 * @since 5.0.5
 */
public class ColumnMapRowMapperTests {

	@Test
	public void returnValueOfFirstEquallyNamedColumn() throws SQLException {

		ColumnMapRowMapper rowMapper = new ColumnMapRowMapper();

		ResultSet resultSet = mock(ResultSet.class);
		ResultSetMetaData metaData = mock(ResultSetMetaData.class);
		given(metaData.getColumnCount()).willReturn(2);
		given(metaData.getColumnLabel(1)).willReturn("x");
		given(metaData.getColumnLabel(2)).willReturn("X");
		given(resultSet.getMetaData()).willReturn(metaData);
		given(resultSet.getObject(1)).willReturn("first value");
		given(resultSet.getObject(2)).willReturn("second value");

		Map<String, Object> map = rowMapper.mapRow(resultSet, 0);
		assertEquals(1, map.size());
		assertEquals(map.get("x"), "first value");

	}

}
