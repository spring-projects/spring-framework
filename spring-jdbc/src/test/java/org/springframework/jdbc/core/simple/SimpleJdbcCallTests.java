/*
 * Copyright 2002-2008 the original author or authors.
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

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.dao.InvalidDataAccessApiUsageException;

import javax.sql.DataSource;
import java.sql.*;

/**
 * Mock object based tests for SimpleJdbcCall.
 *
 * @author Thomas Risberg
 */
public class SimpleJdbcCallTests extends TestCase {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	private MockControl ctrlDataSource;
	private DataSource mockDataSource;
	private MockControl ctrlConnection;
	private Connection mockConnection;
	private MockControl ctrlDatabaseMetaData;
	private DatabaseMetaData mockDatabaseMetaData;
	private MockControl ctrlCallable;
	private CallableStatement mockCallable;

	protected void setUp() throws Exception {
		ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setDefaultReturnValue(mockDatabaseMetaData);
		mockConnection.close();
		ctrlConnection.setDefaultVoidCallable();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setDefaultReturnValue(mockConnection);

		ctrlCallable = MockControl.createControl(CallableStatement.class);
		mockCallable = (CallableStatement) ctrlCallable.getMock();
	}

	protected void tearDown() throws Exception {
		ctrlDatabaseMetaData.verify();
		ctrlDataSource.verify();
		ctrlCallable.verify();
	}

	protected void replay() {
		ctrlDatabaseMetaData.replay();
		ctrlConnection.replay();
		ctrlDataSource.replay();
		ctrlCallable.replay();
	}

	public void testNoSuchStoredProcedure() throws Exception {
		final String NO_SUCH_PROC = "x";

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue("me");
		mockDatabaseMetaData.supportsCatalogsInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.supportsSchemasInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);

		SQLException sex =
			new SQLException(
				"Syntax error or access violation exception",
				"42000");
		mockCallable.execute();
		ctrlCallable.setThrowable(sex);
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall(
			"{call " + NO_SUCH_PROC + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();

		SimpleJdbcCall sproc = new SimpleJdbcCall(mockDataSource).withProcedureName(NO_SUCH_PROC);
		try {
			sproc.execute();
			fail("Shouldn't succeed in running stored procedure which doesn't exist");
		} catch (BadSqlGrammarException ex) {
			// OK
		}
	}

	public void testUnnamedParameterHandling() throws Exception {
		final String MY_PROC = "my_proc";

		replay();

		SimpleJdbcCall sproc = new SimpleJdbcCall(mockDataSource).withProcedureName(MY_PROC);
		try {
			sproc.addDeclaredParameter(new SqlParameter(1));
			fail("Shouldn't succeed in adding unnamed parameter");
		} catch (InvalidDataAccessApiUsageException ex) {
			// OK
		}
	}

	public void testAddInvoiceProcWithoutMetaDataUsingMapParamSource() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		initializeAddInvoiceWithoutMetaData(false);

		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withProcedureName("add_invoice");
		adder.declareParameters(new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource()
				.addValue("amount", amount)
				.addValue("custid", custid));
		assertEquals(4, newId.intValue());
	}

	public void testAddInvoiceProcWithoutMetaDataUsingArrayParams() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		initializeAddInvoiceWithoutMetaData(false);

		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withProcedureName("add_invoice");
		adder.declareParameters(new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER),
				new SqlOutParameter("newid", Types.INTEGER));
		Number newId = adder.executeObject(Number.class, amount, custid);
		assertEquals(4, newId.intValue());
	}

	public void testAddInvoiceProcWithMetaDataUsingMapParamSource() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		MockControl ctrlResultSet = initializeAddInvoiceWithMetaData(false);

		ctrlResultSet.replay();
		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, new MapSqlParameterSource()
				.addValue("amount", amount)
				.addValue("custid", custid));
		assertEquals(4, newId.intValue());

		ctrlResultSet.verify();
	}

	public void testAddInvoiceProcWithMetaDataUsingArrayParams() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		MockControl ctrlResultSet = initializeAddInvoiceWithMetaData(false);

		ctrlResultSet.replay();
		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withProcedureName("add_invoice");
		Number newId = adder.executeObject(Number.class, amount, custid);
		assertEquals(4, newId.intValue());

		ctrlResultSet.verify();
	}

	public void testAddInvoiceFuncWithoutMetaDataUsingMapParamSource() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		initializeAddInvoiceWithoutMetaData(true);

		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withFunctionName("add_invoice");
		adder.declareParameters(new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", amount)
				.addValue("custid", custid));
		assertEquals(4, newId.intValue());
	}

	public void testAddInvoiceFuncWithoutMetaDataUsingArrayParams() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		initializeAddInvoiceWithoutMetaData(true);

		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withFunctionName("add_invoice");
		adder.declareParameters(new SqlOutParameter("return", Types.INTEGER),
				new SqlParameter("amount", Types.INTEGER),
				new SqlParameter("custid", Types.INTEGER));
		Number newId = adder.executeFunction(Number.class, amount, custid);
		assertEquals(4, newId.intValue());
	}

	public void testAddInvoiceFuncWithMetaDataUsingMapParamSource() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		MockControl ctrlResultSet = initializeAddInvoiceWithMetaData(true);

		ctrlResultSet.replay();
		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, new MapSqlParameterSource()
				.addValue("amount", amount)
				.addValue("custid", custid));
		assertEquals(4, newId.intValue());

		ctrlResultSet.verify();
	}

	public void testAddInvoiceFuncWithMetaDataUsingArrayParams() throws Exception {
		final int amount = 1103;
		final int custid = 3;

		MockControl ctrlResultSet = initializeAddInvoiceWithMetaData(true);

		ctrlResultSet.replay();
		replay();

		SimpleJdbcCall adder = new SimpleJdbcCall(mockDataSource).withFunctionName("add_invoice");
		Number newId = adder.executeFunction(Number.class, amount, custid);
		assertEquals(4, newId.intValue());

		ctrlResultSet.verify();
	}

	private void initializeAddInvoiceWithoutMetaData(boolean isFunction) throws SQLException {
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue("me");
		mockDatabaseMetaData.supportsCatalogsInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.supportsSchemasInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);

		if (isFunction) {
			mockCallable.registerOutParameter(1, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.setObject(2, 1103, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.setObject(3, 3, 4);
			ctrlCallable.setVoidCallable();
		}
		else {
			mockCallable.setObject(1, 1103, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.setObject(2, 3, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.registerOutParameter(3, 4);
			ctrlCallable.setVoidCallable();
		}
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (isFunction) {
			mockCallable.getObject(1);
		}
		else {
			mockCallable.getObject(3);
		}
		ctrlCallable.setReturnValue(new Long(4));
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		if (isFunction) {
			mockConnection.prepareCall(
				"{? = call add_invoice(?, ?)}");
			ctrlConnection.setReturnValue(mockCallable);
		}
		else {
			mockConnection.prepareCall(
				"{call add_invoice(?, ?, ?)}");
			ctrlConnection.setReturnValue(mockCallable);
		}
	}

	private MockControl initializeAddInvoiceWithMetaData(boolean isFunction) throws SQLException {
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString("PROCEDURE_CAT");
		ctrlResultSet.setReturnValue(null);
		mockResultSet.getString("PROCEDURE_SCHEM");
		ctrlResultSet.setReturnValue(null);
		mockResultSet.getString("PROCEDURE_NAME");
		ctrlResultSet.setReturnValue("add_invoice");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		if (isFunction) {
			mockResultSet.getString("COLUMN_NAME");
			ctrlResultSet.setReturnValue(null);
			mockResultSet.getInt("COLUMN_TYPE");
			ctrlResultSet.setReturnValue(5);
		}
		else {
			mockResultSet.getString("COLUMN_NAME");
			ctrlResultSet.setReturnValue("amount");
			mockResultSet.getInt("COLUMN_TYPE");
			ctrlResultSet.setReturnValue(1);
		}
		mockResultSet.getInt("DATA_TYPE");
		ctrlResultSet.setReturnValue(4);
		mockResultSet.getString("TYPE_NAME");
		ctrlResultSet.setReturnValue(null);
		mockResultSet.getBoolean("NULLABLE");
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		if (isFunction) {
			mockResultSet.getString("COLUMN_NAME");
			ctrlResultSet.setReturnValue("amount");
			mockResultSet.getInt("COLUMN_TYPE");
			ctrlResultSet.setReturnValue(1);
		}
		else {
			mockResultSet.getString("COLUMN_NAME");
			ctrlResultSet.setReturnValue("custid");
			mockResultSet.getInt("COLUMN_TYPE");
			ctrlResultSet.setReturnValue(1);
		}
		mockResultSet.getInt("DATA_TYPE");
		ctrlResultSet.setReturnValue(4);
		mockResultSet.getString("TYPE_NAME");
		ctrlResultSet.setReturnValue(null);
		mockResultSet.getBoolean("NULLABLE");
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		if (isFunction) {
			mockResultSet.getString("COLUMN_NAME");
			ctrlResultSet.setReturnValue("custid");
			mockResultSet.getInt("COLUMN_TYPE");
			ctrlResultSet.setReturnValue(1);
		}
		else {
			mockResultSet.getString("COLUMN_NAME");
			ctrlResultSet.setReturnValue("newid");
			mockResultSet.getInt("COLUMN_TYPE");
			ctrlResultSet.setReturnValue(4);
		}
		mockResultSet.getInt("DATA_TYPE");
		ctrlResultSet.setReturnValue(4);
		mockResultSet.getString("TYPE_NAME");
		ctrlResultSet.setReturnValue(null);
		mockResultSet.getBoolean("NULLABLE");
		ctrlResultSet.setReturnValue(false);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("Oracle");
		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue("ME");
		mockDatabaseMetaData.supportsCatalogsInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.supportsSchemasInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.getProcedures("", "ME", "ADD_INVOICE");
		ctrlDatabaseMetaData.setReturnValue(mockResultSet);
		mockDatabaseMetaData.getProcedureColumns("", "ME", "ADD_INVOICE", null);
		ctrlDatabaseMetaData.setReturnValue(mockResultSet);

		if (isFunction) {
			mockCallable.registerOutParameter(1, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.setObject(2, 1103, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.setObject(3, 3, 4);
			ctrlCallable.setVoidCallable();
		}
		else {
			mockCallable.setObject(1, 1103, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.setObject(2, 3, 4);
			ctrlCallable.setVoidCallable();
			mockCallable.registerOutParameter(3, 4);
			ctrlCallable.setVoidCallable();
		}
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (isFunction) {
			mockCallable.getObject(1);
			ctrlCallable.setReturnValue(new Long(4));
		}
		else {
			mockCallable.getObject(3);
			ctrlCallable.setReturnValue(new Long(4));
		}
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		if (isFunction) {
			mockConnection.prepareCall(
				"{? = call ADD_INVOICE(?, ?)}");
			ctrlConnection.setReturnValue(mockCallable);
		}
		else {
			mockConnection.prepareCall(
				"{call ADD_INVOICE(?, ?, ?)}");
			ctrlConnection.setReturnValue(mockCallable);
		}
		return ctrlResultSet;
	}

}
