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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.tests.Matchers.*;

/**
 * Tests for {@link SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @author Kiril Nugmanov
 */
public class SimpleJdbcCallTests {

	private Connection connection;

	private DatabaseMetaData databaseMetaData;

	private DataSource dataSource;

	private CallableStatement callableStatement;

	@Rule
	public ExpectedException thrown = ExpectedException.none();


	@Before
	public void setUp() throws Exception {
		connection = mock(Connection.class);
		databaseMetaData = mock(DatabaseMetaData.class);
		dataSource = mock(DataSource.class);
		callableStatement = mock(CallableStatement.class);
		given(connection.getMetaData()).willReturn(databaseMetaData);
		given(dataSource.getConnection()).willReturn(connection);
	}


	@Test
	public void testNoSuchStoredProcedure() throws Exception {
		final String NO_SUCH_PROC = "x";
		SQLException sqlException = new SQLException("Syntax error or access violation exception", "42000");
		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getUserName()).willReturn("me");
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(callableStatement.execute()).willThrow(sqlException);
		given(connection.prepareCall("{call " + NO_SUCH_PROC + "()}")).willReturn(callableStatement);
		SimpleJdbcCall sproc = new SimpleJdbcCall(dataSource).withProcedureName(NO_SUCH_PROC);
		thrown.expect(BadSqlGrammarException.class);
		thrown.expect(exceptionCause(sameInstance(sqlException)));
		try {
			sproc.execute();
		}
		finally {
			verify(callableStatement).close();
			verify(connection, atLeastOnce()).close();
		}
	}

	@Test
	public void testUnnamedParameterHandling() throws Exception {
		final String MY_PROC = "my_proc";
		SimpleJdbcCall sproc = new SimpleJdbcCall(dataSource).withProcedureName(MY_PROC);
		// Shouldn't succeed in adding unnamed parameter
		thrown.expect(InvalidDataAccessApiUsageException.class);
		sproc.addDeclaredParameter(new SqlParameter(1));
	}

	@Test
	public void testAddInvoiceProcWithoutMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithoutMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		adder.declareParameters(
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource().
				addValue("amount", 1103).
				addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceProcWithoutMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithoutMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		adder.declareParameters(
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceProcWithMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceProcWithMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceFuncWithoutMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithoutMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		adder.declareParameters(
				new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(true);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceFuncWithoutMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithoutMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		adder.declareParameters(
				new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithoutMetaData(true);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	public void testAddInvoiceFuncWithMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(true);
		verify(connection, atLeastOnce()).close();

	}

	@Test
	public void testAddInvoiceFuncWithMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, 1103, 3);
		assertEquals(4, newId.intValue());
		verifyAddInvoiceWithMetaData(true);
		verify(connection, atLeastOnce()).close();

	}

	@Test
	public void testCorrectFunctionStatement() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{? = call ADD_INVOICE(?, ?)}");
	}

	@Test
	public void testCorrectFunctionStatementNamed() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withFunctionName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{? = call ADD_INVOICE(AMOUNT => ?, CUSTID => ?)}");
	}

	@Test
	public void testCorrectProcedureStatementNamed() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withProcedureName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{call ADD_INVOICE(AMOUNT => ?, CUSTID => ?, NEWID => ?)}");
	}


	private void verifyStatement(SimpleJdbcCall adder, String expected) {
		Assert.assertEquals("Incorrect call statement", expected, adder.getCallString());
	}

	private void initializeAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getUserName()).willReturn("me");
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(callableStatement.execute()).willReturn(false);
		given(callableStatement.getUpdateCount()).willReturn(-1);
		if (isFunction) {
			given(callableStatement.getObject(1)).willReturn(4L);
			given(connection.prepareCall("{? = call add_invoice(?, ?)}")
					).willReturn(callableStatement);
		}
		else {
			given(callableStatement.getObject(3)).willReturn(4L);
			given(connection.prepareCall("{call add_invoice(?, ?, ?)}")
					).willReturn(callableStatement);
		}
	}

	private void verifyAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
		if (isFunction) {
			verify(callableStatement).registerOutParameter(1, 4);
			verify(callableStatement).setObject(2, 1103, 4);
			verify(callableStatement).setObject(3, 3, 4);
		}
		else {
			verify(callableStatement).setObject(1, 1103, 4);
			verify(callableStatement).setObject(2, 3, 4);
			verify(callableStatement).registerOutParameter(3, 4);
		}
		verify(callableStatement).close();
	}

	private void initializeAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
		ResultSet proceduresResultSet = mock(ResultSet.class);
		ResultSet procedureColumnsResultSet = mock(ResultSet.class);
		given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
		given(databaseMetaData.getUserName()).willReturn("ME");
		given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
		given(databaseMetaData.getProcedures("", "ME", "ADD_INVOICE")).willReturn(proceduresResultSet);
		given(databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null)).willReturn(procedureColumnsResultSet);

		given(proceduresResultSet.next()).willReturn(true, false);
		given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("add_invoice");

		given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
		given(procedureColumnsResultSet.getInt("DATA_TYPE")).willReturn(4);
		if (isFunction) {
			given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn(null,"amount", "custid");
			given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(5, 1, 1);
			given(connection.prepareCall("{? = call ADD_INVOICE(?, ?)}")).willReturn(callableStatement);
			given(callableStatement.getObject(1)).willReturn(4L);
		}
		else {
			given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn("amount", "custid", "newid");
			given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(1, 1, 4);
			given(connection.prepareCall("{call ADD_INVOICE(?, ?, ?)}")).willReturn(callableStatement);
			given(callableStatement.getObject(3)).willReturn(4L);
		}
		given(callableStatement.getUpdateCount()).willReturn(-1);
	}

	private void verifyAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
		ResultSet proceduresResultSet = databaseMetaData.getProcedures("", "ME", "ADD_INVOICE");
		ResultSet procedureColumnsResultSet = databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null);
		if (isFunction) {
			verify(callableStatement).registerOutParameter(1, 4);
			verify(callableStatement).setObject(2, 1103, 4);
			verify(callableStatement).setObject(3, 3, 4);
		}
		else {
			verify(callableStatement).setObject(1, 1103, 4);
			verify(callableStatement).setObject(2, 3, 4);
			verify(callableStatement).registerOutParameter(3, 4);
		}
		verify(callableStatement).close();
		verify(proceduresResultSet).close();
		verify(procedureColumnsResultSet).close();
	}

}
