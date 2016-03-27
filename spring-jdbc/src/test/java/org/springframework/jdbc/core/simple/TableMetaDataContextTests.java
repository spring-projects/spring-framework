/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.metadata.TableMetaDataContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Mock object based tests for TableMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class TableMetaDataContextTests  {

	private Connection connection;

	private DataSource dataSource;

	private DatabaseMetaData databaseMetaData;

	private TableMetaDataContext context = new TableMetaDataContext();


	@Before
	public void setUp() throws Exception {
		connection = mock(Connection.class);
		dataSource = mock(DataSource.class);
		databaseMetaData = mock(DatabaseMetaData.class);
		given(connection.getMetaData()).willReturn(databaseMetaData);
		given(dataSource.getConnection()).willReturn(connection);
	}


	@Test
	public void testMatchInParametersAndSqlTypeInfoWrapping() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";

		ResultSet metaDataResultSet = mock(ResultSet.class);
		given(metaDataResultSet.next()).willReturn(true, false);
		given(metaDataResultSet.getString("TABLE_SCHEM")).willReturn(USER);
		given(metaDataResultSet.getString("TABLE_NAME")).willReturn(TABLE);
		given(metaDataResultSet.getString("TABLE_TYPE")).willReturn("TABLE");

		ResultSet columnsResultSet = mock(ResultSet.class);
		given(columnsResultSet.next()).willReturn(
				true, true, true, true, false);
		given(columnsResultSet.getString("COLUMN_NAME")).willReturn(
				"id", "name", "customersince", "version");
		given(columnsResultSet.getInt("DATA_TYPE")).willReturn(
				Types.INTEGER, Types.VARCHAR, Types.DATE, Types.NUMERIC);
		given(columnsResultSet.getBoolean("NULLABLE")).willReturn(
				false, true, true, false);

		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getDatabaseProductName()).willReturn("1.0");
		given(databaseMetaData.getUserName()).willReturn(USER);
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(databaseMetaData.getTables(null, null, TABLE, null)).willReturn(metaDataResultSet);
		given(databaseMetaData.getColumns(null, USER, TABLE, null)).willReturn(columnsResultSet);

		MapSqlParameterSource map = new MapSqlParameterSource();
		map.addValue("id", 1);
		map.addValue("name", "Sven");
		map.addValue("customersince", new Date());
		map.addValue("version", 0);
		map.registerSqlType("customersince", Types.DATE);
		map.registerSqlType("version", Types.NUMERIC);

		context.setTableName(TABLE);
		context.processMetaData(dataSource, new ArrayList<String>(), new String[] {});

		List<Object> values = context.matchInParameterValuesWithInsertColumns(map);

		assertEquals("wrong number of parameters: ", 4, values.size());
		assertTrue("id not wrapped with type info", values.get(0) instanceof Number);
		assertTrue("name not wrapped with type info", values.get(1) instanceof String);
		assertTrue("date wrapped with type info",
				values.get(2) instanceof SqlParameterValue);
		assertTrue("version wrapped with type info",
				values.get(3) instanceof SqlParameterValue);
		verify(metaDataResultSet, atLeastOnce()).next();
		verify(columnsResultSet, atLeastOnce()).next();
		verify(metaDataResultSet).close();
		verify(columnsResultSet).close();
	}

	@Test
	public void testTableWithSingleColumnGeneratedKey() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";

		ResultSet metaDataResultSet = mock(ResultSet.class);
		given(metaDataResultSet.next()).willReturn(true, false);
		given(metaDataResultSet.getString("TABLE_SCHEM")).willReturn(USER);
		given(metaDataResultSet.getString("TABLE_NAME")).willReturn(TABLE);
		given(metaDataResultSet.getString("TABLE_TYPE")).willReturn("TABLE");

		ResultSet columnsResultSet = mock(ResultSet.class);
		given(columnsResultSet.next()).willReturn(true, false);
		given(columnsResultSet.getString("COLUMN_NAME")).willReturn("id");
		given(columnsResultSet.getInt("DATA_TYPE")).willReturn(Types.INTEGER);
		given(columnsResultSet.getBoolean("NULLABLE")).willReturn(false);

		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getDatabaseProductName()).willReturn("1.0");
		given(databaseMetaData.getUserName()).willReturn(USER);
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(databaseMetaData.getTables(null, null, TABLE, null)).willReturn(metaDataResultSet);
		given(databaseMetaData.getColumns(null, USER, TABLE, null)).willReturn(columnsResultSet);

		MapSqlParameterSource map = new MapSqlParameterSource();
		String[] keyCols = new String[] { "id" };
		context.setTableName(TABLE);
		context.processMetaData(dataSource, new ArrayList<String>(), keyCols);
		List<Object> values = context.matchInParameterValuesWithInsertColumns(map);
		String insertString = context.createInsertString(keyCols);

		assertEquals("wrong number of parameters: ", 0, values.size());
		assertEquals("empty insert not generated correctly", "INSERT INTO customers () VALUES()", insertString);
		verify(metaDataResultSet, atLeastOnce()).next();
		verify(columnsResultSet, atLeastOnce()).next();
		verify(metaDataResultSet).close();
		verify(columnsResultSet).close();
	}

}
