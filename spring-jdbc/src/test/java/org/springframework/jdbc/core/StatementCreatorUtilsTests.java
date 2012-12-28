/*
 * Copyright 2002-2012 the original author or authors.
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;

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

	@Test public void testSetParameterValueWithNullAndType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, null, null);
		verify(preparedStatement).setNull(1, Types.VARCHAR);
	}

	@Test public void testSetParameterValueWithNullAndTypeName() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.VARCHAR, "mytype", null);
		verify(preparedStatement).setNull(1, Types.VARCHAR, "mytype");
	}

	@Test public void testSetParameterValueWithNullAndUnknownType() throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(preparedStatement).setNull(1, Types.NULL);
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownTypeOnInformix() throws SQLException {
		Connection con = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(metaData);
		given(metaData.getDatabaseProductName()).willReturn("Informix Dynamic Server");
		given(metaData.getDriverName()).willReturn("Informix Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(metaData).getDatabaseProductName();
		verify(metaData).getDriverName();
		verify(preparedStatement).setObject(1, null);
	}

	@Test public void testSetParameterValueWithNullAndUnknownTypeOnDerbyEmbedded() throws SQLException {
		Connection con = mock(Connection.class);
		DatabaseMetaData metaData = mock(DatabaseMetaData.class);
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(metaData);
		given(metaData.getDatabaseProductName()).willReturn("Apache Derby");
		given(metaData.getDriverName()).willReturn("Apache Derby Embedded Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(metaData).getDatabaseProductName();
		verify(metaData).getDriverName();
		verify(preparedStatement).setNull(1, Types.VARCHAR);
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

}
