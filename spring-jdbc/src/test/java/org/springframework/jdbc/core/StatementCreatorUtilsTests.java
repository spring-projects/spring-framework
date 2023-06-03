/*
 * Copyright 2002-2023 the original author or authors.
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.GregorianCalendar;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;
import static org.mockito.BDDMockito.doThrow;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 31.08.2004
 */
public class StatementCreatorUtilsTests {

	private PreparedStatement preparedStatement = mock();


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
		StatementCreatorUtils.shouldIgnoreGetParameterType = true;
		Connection con = mock();
		DatabaseMetaData dbmd = mock();
		given(preparedStatement.getConnection()).willReturn(con);
		given(dbmd.getDatabaseProductName()).willReturn("Oracle");
		given(dbmd.getDriverName()).willReturn("Oracle Driver");
		given(con.getMetaData()).willReturn(dbmd);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(preparedStatement).setNull(1, Types.NULL);
		StatementCreatorUtils.shouldIgnoreGetParameterType = false;
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownTypeOnInformix() throws SQLException {
		StatementCreatorUtils.shouldIgnoreGetParameterType = true;
		Connection con = mock();
		DatabaseMetaData dbmd = mock();
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDatabaseProductName()).willReturn("Informix Dynamic Server");
		given(dbmd.getDriverName()).willReturn("Informix Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setObject(1, null);
		StatementCreatorUtils.shouldIgnoreGetParameterType = false;
	}

	@Test
	public void testSetParameterValueWithNullAndUnknownTypeOnDerbyEmbedded() throws SQLException {
		StatementCreatorUtils.shouldIgnoreGetParameterType = true;
		Connection con = mock();
		DatabaseMetaData dbmd = mock();
		given(preparedStatement.getConnection()).willReturn(con);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getDatabaseProductName()).willReturn("Apache Derby");
		given(dbmd.getDriverName()).willReturn("Apache Derby Embedded Driver");
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(dbmd).getDatabaseProductName();
		verify(dbmd).getDriverName();
		verify(preparedStatement).setNull(1, Types.VARCHAR);
		StatementCreatorUtils.shouldIgnoreGetParameterType = false;
	}

	@Test
	public void testSetParameterValueWithNullAndGetParameterTypeWorking() throws SQLException {
		ParameterMetaData pmd = mock();
		given(preparedStatement.getParameterMetaData()).willReturn(pmd);
		given(pmd.getParameterType(1)).willReturn(Types.SMALLINT);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, SqlTypeValue.TYPE_UNKNOWN, null, null);
		verify(pmd).getParameterType(1);
		verify(preparedStatement, never()).getConnection();
		verify(preparedStatement).setNull(1, Types.SMALLINT);
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

	@ParameterizedTest
	@MethodSource("javaTimeTypes")
	public void testSetParameterValueWithJavaTimeTypes(Object o, int sqlType) throws SQLException {
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, sqlType, null, o);
		verify(preparedStatement).setObject(1, o, sqlType);
	}

	@ParameterizedTest
	@MethodSource("javaTimeTypes")
	void javaTimeTypesToSqlParameterType(Object o, int expectedSqlType) {
		assertThat(StatementCreatorUtils.javaTypeToSqlParameterType(o.getClass()))
				.isEqualTo(expectedSqlType);
	}

	static Stream<Arguments> javaTimeTypes() {
		ZoneOffset PLUS_NINE = ZoneOffset.ofHours(9);
		final LocalDateTime now = LocalDateTime.now();
		return Stream.of(
				Arguments.of(named("LocalTime", LocalTime.NOON), named("TIME", Types.TIME)),
				Arguments.of(named("LocalDate", LocalDate.EPOCH), named("DATE", Types.DATE)),
				Arguments.of(named("LocalDateTime", now), named("TIMESTAMP", Types.TIMESTAMP)),
				Arguments.of(named("OffsetTime", LocalTime.NOON.atOffset(PLUS_NINE)),
						named("TIME_WITH_TIMEZONE", Types.TIME_WITH_TIMEZONE)),
				Arguments.of(named("OffsetDateTime", now.atOffset(PLUS_NINE)),
						named("TIMESTAMP_WITH_TIMEZONE", Types.TIMESTAMP_WITH_TIMEZONE))
		);
	}

	@Test  // gh-30556
	public void testSetParameterValueWithOffsetDateTimeAndNotSupported() throws SQLException {
		OffsetDateTime time = OffsetDateTime.now();
		doThrow(new SQLFeatureNotSupportedException()).when(preparedStatement).setObject(1, time, Types.TIMESTAMP_WITH_TIMEZONE);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP_WITH_TIMEZONE, null, time);
		verify(preparedStatement).setObject(1, time, Types.TIMESTAMP_WITH_TIMEZONE);
		verify(preparedStatement).setObject(1, time);
	}

	@Test  // gh-30556
	public void testSetParameterValueWithNullAndNotSupported() throws SQLException {
		doThrow(new SQLFeatureNotSupportedException()).when(preparedStatement).setNull(1, Types.TIMESTAMP_WITH_TIMEZONE);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.TIMESTAMP_WITH_TIMEZONE, null, null);
		verify(preparedStatement).setNull(1, Types.TIMESTAMP_WITH_TIMEZONE);
		verify(preparedStatement).setNull(1, Types.NULL);
	}

	@Test  // SPR-8571
	public void testSetParameterValueWithStringAndVendorSpecificType() throws SQLException {
		Connection con = mock();
		DatabaseMetaData dbmd = mock();
		given(preparedStatement.getConnection()).willReturn(con);
		given(dbmd.getDatabaseProductName()).willReturn("Oracle");
		given(con.getMetaData()).willReturn(dbmd);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, "test");
		verify(preparedStatement).setString(1, "test");
	}

	@Test  // SPR-8571
	public void testSetParameterValueWithNullAndVendorSpecificType() throws SQLException {
		StatementCreatorUtils.shouldIgnoreGetParameterType = true;
		Connection con = mock();
		DatabaseMetaData dbmd = mock();
		given(preparedStatement.getConnection()).willReturn(con);
		given(dbmd.getDatabaseProductName()).willReturn("Oracle");
		given(dbmd.getDriverName()).willReturn("Oracle Driver");
		given(con.getMetaData()).willReturn(dbmd);
		StatementCreatorUtils.setParameterValue(preparedStatement, 1, Types.OTHER, null, null);
		verify(preparedStatement).setNull(1, Types.NULL);
		StatementCreatorUtils.shouldIgnoreGetParameterType = false;
	}

}
