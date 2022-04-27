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

package org.springframework.jdbc.support.rowset;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.InvalidResultSetAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Thomas Risberg
 */
public class ResultSetWrappingRowSetTests {

	private ResultSet resultSet;

	private ResultSetWrappingSqlRowSet rowSet;


	@BeforeEach
	public void setup() throws Exception {
		resultSet = mock(ResultSet.class);
		rowSet = new ResultSetWrappingSqlRowSet(resultSet);
	}


	@Test
	public void testGetBigDecimalInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBigDecimal", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBigDecimal", int.class);
		doTest(rset, rowset, 1, BigDecimal.ONE);
	}

	@Test
	public void testGetBigDecimalString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBigDecimal", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBigDecimal", String.class);
		doTest(rset, rowset, "test", BigDecimal.ONE);
	}

	@Test
	public void testGetStringInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getString", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getString", int.class);
		doTest(rset, rowset, 1, "test");
	}

	@Test
	public void testGetStringString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getString", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getString", String.class);
		doTest(rset, rowset, "test", "test");
	}

	@Test
	public void testGetTimestampInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTimestamp", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTimestamp", int.class);
		doTest(rset, rowset, 1, new Timestamp(1234L));
	}

	@Test
	public void testGetTimestampString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTimestamp", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTimestamp", String.class);
		doTest(rset, rowset, "test", new Timestamp(1234L));
	}

	@Test
	public void testGetDateInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDate", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDate", int.class);
		doTest(rset, rowset, 1, new Date(1234L));
	}

	@Test
	public void testGetDateString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDate", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDate", String.class);
		doTest(rset, rowset, "test", new Date(1234L));
	}

	@Test
	public void testGetTimeInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTime", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTime", int.class);
		doTest(rset, rowset, 1, new Time(1234L));
	}

	@Test
	public void testGetTimeString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTime", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTime", String.class);
		doTest(rset, rowset, "test", new Time(1234L));
	}

	@Test
	public void testGetObjectInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getObject", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getObject", int.class);
		doTest(rset, rowset, 1, new Object());
	}

	@Test
	public void testGetObjectString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getObject", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getObject", String.class);
		doTest(rset, rowset, "test", new Object());
	}

	@Test
	public void testGetIntInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getInt", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getInt", int.class);
		doTest(rset, rowset, 1, 1);
	}

	@Test
	public void testGetIntString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getInt", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getInt", String.class);
		doTest(rset, rowset, "test", 1);
	}

	@Test
	public void testGetFloatInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getFloat", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getFloat", int.class);
		doTest(rset, rowset, 1, 1.0f);
	}

	@Test
	public void testGetFloatString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getFloat", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getFloat", String.class);
		doTest(rset, rowset, "test", 1.0f);
	}

	@Test
	public void testGetDoubleInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDouble", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDouble", int.class);
		doTest(rset, rowset, 1, 1.0d);
	}

	@Test
	public void testGetDoubleString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDouble", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDouble", String.class);
		doTest(rset, rowset, "test", 1.0d);
	}

	@Test
	public void testGetLongInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getLong", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getLong", int.class);
		doTest(rset, rowset, 1, 1L);
	}

	@Test
	public void testGetLongString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getLong", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getLong", String.class);
		doTest(rset, rowset, "test", 1L);
	}

	@Test
	public void testGetBooleanInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBoolean", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBoolean", int.class);
		doTest(rset, rowset, 1, true);
	}

	@Test
	public void testGetBooleanString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBoolean", int.class);
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBoolean", String.class);
		doTest(rset, rowset, "test", true);
	}

	private void doTest(Method rsetMethod, Method rowsetMethod, Object arg, Object ret) throws Exception {
		if (arg instanceof String) {
			given(resultSet.findColumn((String) arg)).willReturn(1);
			given(rsetMethod.invoke(resultSet, 1)).willReturn(ret).willThrow(new SQLException("test"));
		}
		else {
			given(rsetMethod.invoke(resultSet, arg)).willReturn(ret).willThrow(new SQLException("test"));
		}
		rowsetMethod.invoke(rowSet, arg);
		assertThatExceptionOfType(InvocationTargetException.class).isThrownBy(() ->
				rowsetMethod.invoke(rowSet, arg)).
			satisfies(ex -> assertThat(ex.getTargetException()).isExactlyInstanceOf(InvalidResultSetAccessException.class));
	}

}
