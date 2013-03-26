/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jdbc.support.rowset;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.InvalidResultSetAccessException;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thomas Risberg
 */
public class ResultSetWrappingRowSetTests {

	private ResultSet rset;
	private ResultSetWrappingSqlRowSet rowset;

	@Before
	public void setUp() throws Exception {
		rset = mock(ResultSet.class);
		rowset = new ResultSetWrappingSqlRowSet(rset);
	}

	@Test
	public void testGetBigDecimalInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBigDecimal", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBigDecimal", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), BigDecimal.valueOf(1));
	}

	@Test
	public void testGetBigDecimalString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBigDecimal", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBigDecimal", new Class[] {String.class});
		doTest(rset, rowset, "test", BigDecimal.valueOf(1));
	}

	@Test
	public void testGetStringInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getString", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getString", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), "test");
	}

	@Test
	public void testGetStringString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getString", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getString", new Class[] {String.class});
		doTest(rset, rowset, "test", "test");
	}

	@Test
	public void testGetTimestampInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTimestamp", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTimestamp", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Timestamp(1234l));
	}

	@Test
	public void testGetTimestampString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTimestamp", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTimestamp", new Class[] {String.class});
		doTest(rset, rowset, "test", new Timestamp(1234l));
	}

	@Test
	public void testGetDateInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDate", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDate", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Date(1234l));
	}

	@Test
	public void testGetDateString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDate", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDate", new Class[] {String.class});
		doTest(rset, rowset, "test", new Date(1234l));
	}

	@Test
	public void testGetTimeInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTime", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTime", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Time(1234l));
	}

	@Test
	public void testGetTimeString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getTime", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getTime", new Class[] {String.class});
		doTest(rset, rowset, "test", new Time(1234l));
	}

	@Test
	public void testGetObjectInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getObject", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getObject", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Object());
	}

	@Test
	public void testGetObjectString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getObject", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getObject", new Class[] {String.class});
		doTest(rset, rowset, "test", new Object());
	}

	@Test
	public void testGetIntInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getInt", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getInt", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Integer(1));
	}

	@Test
	public void testGetIntString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getInt", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getInt", new Class[] {String.class});
		doTest(rset, rowset, "test", new Integer(1));
	}

	@Test
	public void testGetFloatInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getFloat", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getFloat", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Float(1));
	}

	@Test
	public void testGetFloatString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getFloat", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getFloat", new Class[] {String.class});
		doTest(rset, rowset, "test", new Float(1));
	}

	@Test
	public void testGetDoubleInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDouble", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDouble", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Double(1));
	}

	@Test
	public void testGetDoubleString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getDouble", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getDouble", new Class[] {String.class});
		doTest(rset, rowset, "test", new Double(1));
	}

	@Test
	public void testGetLongInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getLong", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getLong", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Long(1));
	}

	@Test
	public void testGetLongString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getLong", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getLong", new Class[] {String.class});
		doTest(rset, rowset, "test", new Long(1));
	}

	@Test
	public void testGetBooleanInt() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBoolean", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBoolean", new Class[] {int.class});
		doTest(rset, rowset, new Integer(1), new Boolean(true));
	}

	@Test
	public void testGetBooleanString() throws Exception {
		Method rset = ResultSet.class.getDeclaredMethod("getBoolean", new Class[] {int.class});
		Method rowset = ResultSetWrappingSqlRowSet.class.getDeclaredMethod("getBoolean", new Class[] {String.class});
		doTest(rset, rowset, "test", new Boolean(true));
	}

	private void doTest(Method rsetMethod, Method rowsetMethod, Object arg, Object ret) throws Exception {
		if (arg instanceof String) {
			given(rset.findColumn((String) arg)).willReturn(1);
			given(rsetMethod.invoke(rset, 1)).willReturn(ret).willThrow(new SQLException("test"));
		} else {
			given(rsetMethod.invoke(rset, arg)).willReturn(ret).willThrow(new SQLException("test"));
		}
		rowsetMethod.invoke(rowset, arg);
		try {
			rowsetMethod.invoke(rowset, arg);
			fail("InvalidResultSetAccessException should have been thrown");
		}
		catch (InvocationTargetException ex) {
			assertEquals(InvalidResultSetAccessException.class, ex.getTargetException().getClass());
		}
	}

}
