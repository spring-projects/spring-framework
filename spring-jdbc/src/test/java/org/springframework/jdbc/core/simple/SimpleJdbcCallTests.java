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

package org.springframework.jdbc.core.simple;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @author Kiril Nugmanov
 * @author Sam Brannen
 */
class SimpleJdbcCallTests {

	private final Connection connection = mock(Connection.class);

	private final DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);

	private final DataSource dataSource = mock(DataSource.class);

	private final CallableStatement callableStatement = mock(CallableStatement.class);


	@BeforeEach
	void setUp() throws Exception {
		given(connection.getMetaData()).willReturn(databaseMetaData);
		given(dataSource.getConnection()).willReturn(connection);
	}


	@Test
	void noSuchStoredProcedure() throws Exception {
		final String NO_SUCH_PROC = "x";
		SQLException sqlException = new SQLException("Syntax error or access violation exception", "42000");
		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getUserName()).willReturn("me");
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
		given(callableStatement.execute()).willThrow(sqlException);
		given(connection.prepareCall("{call " + NO_SUCH_PROC + "()}")).willReturn(callableStatement);
		SimpleJdbcCall sproc = new SimpleJdbcCall(dataSource).withProcedureName(NO_SUCH_PROC);
		try {
			assertThatExceptionOfType(BadSqlGrammarException.class)
				.isThrownBy(() -> sproc.execute())
				.withCause(sqlException);
		}
		finally {
			verify(callableStatement).close();
			verify(connection, atLeastOnce()).close();
		}
	}

	@Test
	void unnamedParameterHandling() throws Exception {
		final String MY_PROC = "my_proc";
		SimpleJdbcCall sproc = new SimpleJdbcCall(dataSource).withProcedureName(MY_PROC);
		// Shouldn't succeed in adding unnamed parameter
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				sproc.addDeclaredParameter(new SqlParameter(1)));
	}

	@Test
	void addInvoiceProcWithoutMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithoutMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		adder.declareParameters(
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource().
				addValue("amount", 1103).
				addValue("custid", 3));
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithoutMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceProcWithoutMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithoutMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		adder.declareParameters(
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, 1103, 3);
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithoutMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceProcWithMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceProcWithMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, 1103, 3);
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithMetaData(false);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceFuncWithoutMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithoutMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		adder.declareParameters(
				new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithoutMetaData(true);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceFuncWithoutMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithoutMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		adder.declareParameters(
				new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, 1103, 3);
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithoutMetaData(true);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceFuncWithMetaDataUsingMapParamSource() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", 1103)
				.addValue("custid", 3));
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithMetaData(true);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void addInvoiceFuncWithMetaDataUsingArrayParams() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, 1103, 3);
		assertThat(newId.intValue()).isEqualTo(4);
		verifyAddInvoiceWithMetaData(true);
		verify(connection, atLeastOnce()).close();
	}

	@Test
	void correctFunctionStatement() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withFunctionName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{? = call ADD_INVOICE(?, ?)}");
	}

	@Test
	void correctFunctionStatementNamed() throws Exception {
		initializeAddInvoiceWithMetaData(true);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withFunctionName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{? = call ADD_INVOICE(AMOUNT => ?, CUSTID => ?)}");
	}

	@Test
	void correctProcedureStatementNamed() throws Exception {
		initializeAddInvoiceWithMetaData(false);
		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withProcedureName("add_invoice");
		adder.compile();
		verifyStatement(adder, "{call ADD_INVOICE(AMOUNT => ?, CUSTID => ?, NEWID => ?)}");
	}

	/**
	 * This test demonstrates that a CALL statement will still be generated if
	 * an exception occurs while retrieving metadata, potentially resulting in
	 * missing metadata and consequently a failure while invoking the stored
	 * procedure.
	 */
	@Test  // gh-26486
	void exceptionThrownWhileRetrievingColumnNamesFromMetadata() throws Exception {
		ResultSet proceduresResultSet = mock(ResultSet.class);
		ResultSet procedureColumnsResultSet = mock(ResultSet.class);

		given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
		given(databaseMetaData.getUserName()).willReturn("ME");
		given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
		given(databaseMetaData.getProcedures("", "ME", "ADD_INVOICE")).willReturn(proceduresResultSet);
		given(databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null)).willReturn(procedureColumnsResultSet);

		ResultSetMetaData procedureColumnsResultSetMetadata = mock(ResultSetMetaData.class);
		given(procedureColumnsResultSet.getMetaData()).willReturn(procedureColumnsResultSetMetadata);

		given(procedureColumnsResultSetMetadata.getColumnCount()).willReturn(22);
		given(procedureColumnsResultSetMetadata.getColumnName(22)).willReturn("OVERLOAD");

		given(proceduresResultSet.next()).willReturn(true, false);
		given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("add_invoice");

		given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
		given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn("amount", "custid", "newid");
		given(procedureColumnsResultSet.getInt("DATA_TYPE"))
			// Return a valid data type for the first 2 columns.
			.willReturn(Types.INTEGER, Types.INTEGER)
			// 3rd time, simulate an error while retrieving metadata.
			.willThrow(new SQLException("error with DATA_TYPE for column 3"));

		SimpleJdbcCall adder = new SimpleJdbcCall(dataSource).withNamedBinding().withProcedureName("add_invoice");
		adder.compile();
		// If an exception were not thrown for column 3, we would expect:
		// {call ADD_INVOICE(AMOUNT => ?, CUSTID => ?, NEWID => ?)}
		verifyStatement(adder, "{call ADD_INVOICE(AMOUNT => ?, CUSTID => ?)}");

		verify(proceduresResultSet).close();
		verify(procedureColumnsResultSet).close();
	}

	/**
	 * Mocking metadata for overloaded function SYS.DBMS_LOCK.REQUEST.
	 */
	@Test
	void workingOverloadedFunction() throws Exception {
		given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
		given(databaseMetaData.getUserName()).willReturn("ME");
		given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);
		given(databaseMetaData.supportsSchemasInProcedureCalls()).willReturn(true);

		ResultSet functionsResultSet = mock(ResultSet.class);
		ResultSet functionColumnsResultSet = mock(ResultSet.class);
		given(databaseMetaData.getFunctions("DBMS_LOCK", "SYS", "REQUEST")).willReturn(functionsResultSet);
		given(databaseMetaData.getFunctionColumns("DBMS_LOCK", "SYS", "REQUEST", null)).willReturn(functionColumnsResultSet);

		given(functionsResultSet.next()).willReturn(true, true, false);
		given(functionsResultSet.getString("FUNCTION_NAME")).willReturn("REQUEST","REQUEST");

		ResultSetMetaData functionColumnsResultSetMetadata = mock(ResultSetMetaData.class);
		given(functionColumnsResultSet.getMetaData()).willReturn(functionColumnsResultSetMetadata);

		given(functionColumnsResultSetMetadata.getColumnCount()).willReturn(22);
		given(functionColumnsResultSetMetadata.getColumnName(22)).willReturn("OVERLOAD");

		given(functionColumnsResultSet.next()).willReturn(true, true, true, true,
				true, true, true, true, true, true, false);

		given(functionColumnsResultSet.getString("COLUMN_NAME"))
				.willReturn(null,"ID", "LOCKMODE", "TIMEOUT", "RELEASE_ON_COMMIT",
							null, "LOCKHANDLE", "LOCKMODE", "TIMEOUT", "RELEASE_ON_COMMIT" );

		given(functionColumnsResultSet.getInt("COLUMN_TYPE"))
				.willReturn(5, 1, 1, 1, 1, 5, 1, 1, 1, 1);

		given(functionColumnsResultSet.getInt("DATA_TYPE"))
				.willReturn(2,2, 2, 2, 1111, 2, 12, 2, 2, 1111);

		given(functionColumnsResultSet.getString("TYPE_NAME"))
				.willReturn("NUMBER","NUMBER", "NUMBER", "NUMBER", "PL/SQL BOOLEAN",
						"NUMBER","VARCHAR2", "NUMBER", "NUMBER", "PL/SQL BOOLEAN");

		given(functionColumnsResultSet.getInt("NULLABLE"))
				.willReturn(1,1, 1, 1, 1, 1, 1, 1, 1, 1);

		given(functionColumnsResultSet.getInt("OVERLOAD"))
				.willReturn(1,1, 1, 1, 1, 2, 2, 2, 2, 2);

		given(connection.prepareCall("{? = call SYS.DBMS_LOCK.REQUEST(lockhandle => ?, lockmode => ?, " +
				"timeout => ?, release_on_commit => ?)}")).willReturn(callableStatement);
		given(callableStatement.execute()).willReturn(false);
		given(callableStatement.getUpdateCount()).willReturn(-1);
		given(callableStatement.getObject(1)).willReturn(BigDecimal.ZERO);

		SimpleJdbcCall lockByName = new SimpleJdbcCall(dataSource)
				.withSchemaName("SYS")
				.withCatalogName("dbms_lock")
				.withFunctionName("request")
				.withNamedBinding()
				.declareParameters(
						new SqlOutParameter("return", Types.NUMERIC),
						new SqlParameter("lockhandle", Types.VARCHAR),
						new SqlParameter("lockmode", Types.NUMERIC),
						new SqlParameter("timeout", Types.NUMERIC),
						new SqlParameter("release_on_commit", Types.OTHER)
				);

		int result = lockByName.executeFunction(BigDecimal.class, new MapSqlParameterSource()
				.addValue("lockhandle", "1073742005107374200589")
				.addValue("lockmode", 6)
				.addValue("timeout", 0)
				.addValue("release_on_commit", false, 252)).intValue();

		assertThat(result).isEqualTo(0);
		verify(callableStatement).close();
		verify(connection, atLeastOnce()).close();
	}

	private void verifyStatement(SimpleJdbcCall adder, String expected) {
		assertThat(adder.getCallString()).as("Incorrect call statement").isEqualTo(expected);
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
		given(databaseMetaData.getDatabaseProductName()).willReturn("Oracle");
		given(databaseMetaData.getUserName()).willReturn("ME");
		given(databaseMetaData.storesUpperCaseIdentifiers()).willReturn(true);

		if(isFunction) {
			ResultSet functionsResultSet = mock(ResultSet.class);
			ResultSet functionColumnsResultSet = mock(ResultSet.class);
			given(databaseMetaData.getFunctions("", "ME", "ADD_INVOICE")).willReturn(functionsResultSet);
			given(databaseMetaData.getFunctionColumns("", "ME", "ADD_INVOICE", null)).willReturn(functionColumnsResultSet);

			given(functionsResultSet.next()).willReturn(true, false);
			given(functionsResultSet.getString("FUNCTION_NAME")).willReturn("add_invoice");

			ResultSetMetaData functionColumnsResultSetMetadata = mock(ResultSetMetaData.class);
			given(functionColumnsResultSet.getMetaData()).willReturn(functionColumnsResultSetMetadata);

			given(functionColumnsResultSetMetadata.getColumnCount()).willReturn(22);
			given(functionColumnsResultSetMetadata.getColumnName(22)).willReturn("OVERLOAD");

			given(functionColumnsResultSet.next()).willReturn(true, true, true, false);
			given(functionColumnsResultSet.getInt("DATA_TYPE")).willReturn(4);

			given(functionColumnsResultSet.getString("COLUMN_NAME")).willReturn(null,"amount", "custid");
			given(functionColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(5, 1, 1);
			given(connection.prepareCall("{? = call ADD_INVOICE(?, ?)}")).willReturn(callableStatement);
			given(callableStatement.getObject(1)).willReturn(4L);
		}
		else {
			ResultSet proceduresResultSet = mock(ResultSet.class);
			ResultSet procedureColumnsResultSet = mock(ResultSet.class);
			given(databaseMetaData.getProcedures("", "ME", "ADD_INVOICE")).willReturn(proceduresResultSet);
			given(databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null)).willReturn(procedureColumnsResultSet);

			given(proceduresResultSet.next()).willReturn(true, false);
			given(proceduresResultSet.getString("PROCEDURE_NAME")).willReturn("add_invoice");

			ResultSetMetaData procedureColumnsResultSetMetadata = mock(ResultSetMetaData.class);
			given(procedureColumnsResultSet.getMetaData()).willReturn(procedureColumnsResultSetMetadata);

			given(procedureColumnsResultSetMetadata.getColumnCount()).willReturn(22);
			given(procedureColumnsResultSetMetadata.getColumnName(22)).willReturn("OVERLOAD");

			given(procedureColumnsResultSet.next()).willReturn(true, true, true, false);
			given(procedureColumnsResultSet.getInt("DATA_TYPE")).willReturn(4);

			given(procedureColumnsResultSet.getString("COLUMN_NAME")).willReturn("amount", "custid", "newid");
			given(procedureColumnsResultSet.getInt("COLUMN_TYPE")).willReturn(1, 1, 4);
			given(connection.prepareCall("{call ADD_INVOICE(?, ?, ?)}")).willReturn(callableStatement);
			given(callableStatement.getObject(3)).willReturn(4L);
		}
		given(callableStatement.getUpdateCount()).willReturn(-1);
	}

	private void verifyAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
		ResultSet proceduresResultSet = isFunction ?
				databaseMetaData.getFunctions("", "ME", "ADD_INVOICE") :
				databaseMetaData.getProcedures("", "ME", "ADD_INVOICE");
		ResultSet procedureColumnsResultSet = isFunction ?
				databaseMetaData.getFunctionColumns("", "ME", "ADD_INVOICE"
				, null) :
				databaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE"
				, null);
		if(isFunction) {
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
