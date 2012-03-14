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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.GregorianCalendar;

import junit.framework.TestCase;
import org.easymock.MockControl;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
public class StatementCreatorUtilsTests extends TestCase {

	private MockControl psControl;
	private PreparedStatement ps;

	protected void setUp() {
		psControl = MockControl.createControl(PreparedStatement.class);
		ps = (PreparedStatement) psControl.getMock();
	}

	protected void tearDown() {
		psControl.verify();
	}

	public void testSetParameterValueWithNullAndType() throws SQLException {
		ps.setNull(1, Types.VARCHAR);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.VARCHAR, null, null);
	}

	public void testSetParameterValueWithNullAndTypeName() throws SQLException {
		ps.setNull(1, Types.VARCHAR, "mytype");
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.VARCHAR, "mytype", null);
	}

	public void testSetParameterValueWithNullAndUnknownType() throws SQLException {
		ps.setNull(1, Types.NULL);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
	}

	public void testSetParameterValueWithNullAndUnknownTypeAndJdbc30Driver() throws SQLException {
		MockControl pmdControl = MockControl.createControl(ParameterMetaData.class);
		ParameterMetaData pmd = (ParameterMetaData) pmdControl.getMock();
		ps.getParameterMetaData();
		psControl.setReturnValue(pmd, 1);
		pmd.getParameterType(1);
		pmdControl.setReturnValue(Types.INTEGER, 1);
		ps.setNull(1, Types.INTEGER);
		psControl.setVoidCallable(1);
		psControl.replay();
		pmdControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
	}

	public void testSetParameterValueWithNullAndUnknownTypeOnInformix() throws SQLException {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl metaDataControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData metaData = (DatabaseMetaData) metaDataControl.getMock();
		ps.getParameterMetaData();
		psControl.setReturnValue(null, 1);
		ps.getConnection();
		psControl.setReturnValue(con, 1);
		con.getMetaData();
		conControl.setReturnValue(metaData, 1);
		metaData.getDatabaseProductName();
		metaDataControl.setReturnValue("Informix Dynamic Server");
		metaData.getDriverName();
		metaDataControl.setReturnValue("Informix Driver");
		ps.setObject(1, null);
		psControl.setVoidCallable(1);
		psControl.replay();
		conControl.replay();
		metaDataControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		conControl.verify();
		metaDataControl.verify();
	}

	public void testSetParameterValueWithNullAndUnknownTypeOnDerbyEmbedded() throws SQLException {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl metaDataControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData metaData = (DatabaseMetaData) metaDataControl.getMock();
		ps.getParameterMetaData();
		psControl.setReturnValue(null, 1);
		ps.getConnection();
		psControl.setReturnValue(con, 1);
		con.getMetaData();
		conControl.setReturnValue(metaData, 1);
		metaData.getDatabaseProductName();
		metaDataControl.setReturnValue("Apache Derby");
		metaData.getDriverName();
		metaDataControl.setReturnValue("Apache Derby Embedded Driver");
		ps.setNull(1, Types.VARCHAR);
		psControl.setVoidCallable(1);
		psControl.replay();
		conControl.replay();
		metaDataControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		conControl.verify();
		metaDataControl.verify();
	}

	public void testSetParameterValueWithString() throws SQLException {
		ps.setString(1, "test");
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.VARCHAR, null, "test");
	}

	public void testSetParameterValueWithStringAndSpecialType() throws SQLException {
		ps.setObject(1, "test", Types.CHAR);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.CHAR, null, "test");
	}

	public void testSetParameterValueWithStringAndUnknownType() throws SQLException {
		ps.setString(1, "test");
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, "test");
	}

	public void testSetParameterValueWithSqlDate() throws SQLException {
		java.sql.Date date = new java.sql.Date(1000);
		ps.setDate(1, date);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.DATE, null, date);
	}

	public void testSetParameterValueWithDateAndUtilDate() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		ps.setDate(1, new java.sql.Date(1000));
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.DATE, null, date);
	}

	public void testSetParameterValueWithDateAndCalendar() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		ps.setDate(1, new java.sql.Date(cal.getTime().getTime()), cal);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.DATE, null, cal);
	}

	public void testSetParameterValueWithSqlTime() throws SQLException {
		java.sql.Time time = new java.sql.Time(1000);
		ps.setTime(1, time);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.TIME, null, time);
	}

	public void testSetParameterValueWithTimeAndUtilDate() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		ps.setTime(1, new java.sql.Time(1000));
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.TIME, null, date);
	}

	public void testSetParameterValueWithTimeAndCalendar() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		ps.setTime(1, new java.sql.Time(cal.getTime().getTime()), cal);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.TIME, null, cal);
	}

	public void testSetParameterValueWithSqlTimestamp() throws SQLException {
		java.sql.Timestamp timestamp = new java.sql.Timestamp(1000);
		ps.setTimestamp(1, timestamp);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.TIMESTAMP, null, timestamp);
	}

	public void testSetParameterValueWithTimestampAndUtilDate() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		ps.setTimestamp(1, new java.sql.Timestamp(1000));
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.TIMESTAMP, null, date);
	}

	public void testSetParameterValueWithTimestampAndCalendar() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		ps.setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, Types.TIMESTAMP, null, cal);
	}

	public void testSetParameterValueWithDateAndUnknownType() throws SQLException {
		java.util.Date date = new java.util.Date(1000);
		ps.setTimestamp(1, new java.sql.Timestamp(1000));
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, date);
	}

	public void testSetParameterValueWithCalendarAndUnknownType() throws SQLException {
		java.util.Calendar cal = new GregorianCalendar();
		ps.setTimestamp(1, new java.sql.Timestamp(cal.getTime().getTime()), cal);
		psControl.setVoidCallable(1);
		psControl.replay();
		StatementCreatorUtils.setParameterValue(ps, 1, SqlTypeValue.TYPE_UNKNOWN, null, cal);
	}

}
