/*
 * Copyright 2002-2016 the original author or authors.
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
public class StatementCreatorUtilsTests {

	private PreparedStatement preparedStatement;


	@Before
	public void setUp() {
		preparedStatement = mock(PreparedStatement.class);
	}


	@Test
	public void testSetParameterValueWithNullAndType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, null);
		verify(preparedStatement).setNull(1, Types.VARCHAR);
	}

	@Test
	public void testSetParameterValueWithNullAndTypeName() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, "mytype", null);
		verify(preparedStatement).setNull(1, Types.VARCHAR, "mytype");
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(preparedStatement).setNull(1, Types.NULL);
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownTypeOnInformix() throws SQLException {
		StatementCreatorUtils.driversWithNoSupportForGetParameterType.clear();
		Connection con = mock(Connection.class);
		DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDatabaseProductName()).willReturn("Informix Dynamic Server");
		given(dbmd.getDriverName()).willReturn("Informix Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setObject(1, null);
		assertEquals(1, StatementCreatorUtils.driversWithNoSupportForGetParameterType.size());
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownTypeOnDerbyEmbedded() throws SQLException {
		StatementCreatorUtils.driversWithNoSupportForGetParameterType.clear();
		Connection con = mock(Connection.class);
		DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDatabaseProductName()).willReturn("Apache Derby");
		given(dbmd.getDriverName()).willReturn("Apache Derby Embedded Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setNull(1, Types.VARCHAR);
		assertEquals(1, StatementCreatorUtils.driversWithNoSupportForGetParameterType.size());
	}

	@Test
	public void testSetParameterValueWithNullAndGetParameterTypeWorking() throws SQLException {
		StatementCreatorUtils.driversWithNoSupportForGetParameterType.clear();
		ParameterMetaData pmd = mock(ParameterMetaData.class);
		given(preparedStatement.getParameterMetaData()).willReturn(pmd);
		given(pmd.getParameterType(1)).willReturn(Types.SMALLINT);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(pmd).getParameterType(1);
		verify(preparedStatement).setNull(1, Types.SMALLINT);
		assertTrue(StatementCreatorUtils.driversWithNoSupportForGetParameterType.isEmpty());
	}

	@Test
	public void testSetParameterValueWithNullAndGetParameterTypeWorkingButNotForOtherDriver() throws SQLException {
		StatementCreatorUtils.driversWithNoSupportForGetParameterType.clear();
		StatementCreatorUtils.driversWithNoSupportForGetParameterType.add("Oracle JDBC Driver");
		Connection con = mock(Connection.class);
		DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
		ParameterMetaData pmd = mock(ParameterMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDriverName()).willReturn("Apache Derby Embedded Driver");
		given(preparedStatement.getParameterMetaData()).willReturn(pmd);
		given(pmd.getParameterType(1)).willReturn(Types.SMALLINT);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(dbmd).getDriverName();
		verify(pmd).getParameterType(1);
		verify(preparedStatement).setNull(1, Types.SMALLINT);
		assertEquals(1, StatementCreatorUtils.driversWithNoSupportForGetParameterType.size());
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownTypeAndGetParameterTypeNotWorking() throws SQLException {
		StatementCreatorUtils.driversWithNoSupportForGetParameterType.clear();
		Connection con = mock(Connection.class);
		DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDatabaseProductName()).willReturn("Apache Derby");
		given(dbmd.getDriverName()).willReturn("Apache Derby Embedded Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setNull(1, Types.VARCHAR);
		assertEquals(1, StatementCreatorUtils.driversWithNoSupportForGetParameterType.size());

		reset(preparedStatement, con, dbmd);
		ParameterMetaData pmd = mock(ParameterMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(preparedStatement.getParameterMetaData()).willReturn(pmd);
		given(pmd.getParameterType(1)).willThrow(new SQLException("unsupported"));
		given(dbmd.getDatabaseProductName()).willReturn("Informix Dynamic Server");
		given(dbmd.getDriverName()).willReturn("Informix Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(pmd).getParameterType(1);
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setObject(1, null);
		assertEquals(2, StatementCreatorUtils.driversWithNoSupportForGetParameterType.size());

		reset(preparedStatement, con, dbmd, pmd);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDatabaseProductName()).willReturn("Informix Dynamic Server");
		given(dbmd.getDriverName()).willReturn("Informix Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(preparedStatement, never()).getParameterMetaData();
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setObject(1, null);
		assertEquals(2, StatementCreatorUtils.driversWithNoSupportForGetParameterType.size());
	}

	@Test
	public void testSetParameterValueWithString() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, "test");
		verify(preparedStatement).setString(1, "test");
	}

	@Test
	public void testSetParameterValueWithStringAndSpecialType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.CHAR, null, "test");
		verify(preparedStatement).setObject(1, "test", Types.CHAR);
	}

	@Test public void testSetParameterValueWithStringAndUnknownType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, "test");
		verify(preparedStatement).setString(1, "test");
	}

	@Test
	public void testSetParameterValueWithSqlDate() throws SQLException {
		java.sql.Date date = new java.sql.Date(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, date);
		verify(preparedStatement).setDate(1, date);
	}

	@Test
	public void testSetParameterValueWithDateAndUtilDate() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, date);
		verify(preparedStatement).setDate(1, new java.sql.Date(1000));
	}

	@Test
	public void testSetParameterValueWithDateAndCalendar() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.DATE, null, cal);
		verify(preparedStatement).setDate(1, new java.sql.Date(cal.getTime().getTime()), cal);
	}

	@Test
	public void testSetParameterValueWithSqlTime() throws SQLException {
		java.sql.Time time = new java.sql.Time(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, time);
		verify(preparedStatement).setTime(1, time);
	}

	@Test
	public void testSetParameterValueWithTimeAndUtilDate() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, date);
		verify(preparedStatement).setTime(1, new java.sql.Time(1000));
	}

	@Test
	public void testSetParameterValueWithTimeAndCalendar() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIME, null, cal);
		verify(preparedStatement).setTime(1, new java.sql.Time(cal.getTime().getTime()), cal);
	}

	@Test
	public void testSetParameterValueWithSqlTimestamp() throws SQLException {
		java.sql.Timestamp timestamp = new java.sql.Timestamp(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, timestamp);
		verify(preparedStatement).setTimestamp(1, timestamp);
	}

	@Test
	public void testSetParameterValueWithTimestampAndUtilDate() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, date);
		verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(1000));
	}

	@Test
	public void testSetParameterValueWithTimestampAndCalendar() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP, null, cal);
		verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
	}

	@Test
	public void testSetParameterValueWithDateAndUnknownType() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, date);
		verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(1000));
	}

	@Test
	public void testSetParameterValueWithCalendarAndUnknownType() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, cal);
		verify(preparedStatement).setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
	}

	@Test  // SPR-8571
	public void testSetParameterValueWithStringAndVendorSpecificType() throws SQLException {
		Connection con = mock(Connection.class);
		DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(dbmd.getDatabaseProductName()).willReturn("Oracle");
		given(con.getMetaData()).willReturn(dbmd);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, "test");
		verify(preparedStatement).setString(1, "test");
	}

	@Test  // SPR-8571
	public void testSetParameterValueWithNullAndVendorSpecificType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, null);
		verify(preparedStatement).setNull(1, Types.NULL);
	}

}
