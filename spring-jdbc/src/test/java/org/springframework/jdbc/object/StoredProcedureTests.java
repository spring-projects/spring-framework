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

package org.springframework.jdbc.object;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import javax.sql.DataSource;

import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SimpleRowCountCallbackHandler;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;
import org.springframework.jdbc.datasource.ConnectionHolder;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Thomas Risberg
 * @author Trevor Cook
 * @author Rod Johnson
 */
public class StoredProcedureTests extends AbstractJdbcTests {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	private MockControl ctrlCallable;
	private CallableStatement mockCallable;

	protected void setUp() throws Exception {
		super.setUp();

		ctrlCallable = MockControl.createControl(CallableStatement.class);
		mockCallable = (CallableStatement) ctrlCallable.getMock();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (shouldVerify()) {
			ctrlCallable.verify();
		}
	}

	protected void replay() {
		super.replay();
		ctrlCallable.replay();
	}

	public void testNoSuchStoredProcedure() throws Exception {
		SQLException sex =
			new SQLException(
				"Syntax error or access violation exception",
				"42000");
		mockCallable.execute();
		ctrlCallable.setThrowable(sex);
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall(
			"{call " + NoSuchStoredProcedure.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();

		NoSuchStoredProcedure sproc = new NoSuchStoredProcedure(mockDataSource);
		try {
			sproc.execute();
			fail("Shouldn't succeed in running stored procedure which doesn't exist");
		} catch (BadSqlGrammarException ex) {
			// OK
		}
	}

	private void testAddInvoice(final int amount, final int custid)
		throws Exception {
		AddInvoice adder = new AddInvoice(mockDataSource);
		int id = adder.execute(amount, custid);
		assertEquals(4, id);
	}

	private void testAddInvoiceUsingObjectArray(final int amount, final int custid)
		throws Exception {
		AddInvoiceUsingObjectArray adder = new AddInvoiceUsingObjectArray(mockDataSource);
		int id = adder.execute(amount, custid);
		assertEquals(5, id);
	}

	public void testAddInvoices() throws Exception {
		mockCallable.setObject(1, new Integer(1106), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.setObject(2, new Integer(3), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.registerOutParameter(3, Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(3);
		ctrlCallable.setReturnValue(new Integer(4));
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + AddInvoice.SQL + "(?, ?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		testAddInvoice(1106, 3);

	}

	public void testAddInvoicesUsingObjectArray() throws Exception {
		mockCallable.setObject(1, new Integer(1106), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.setObject(2, new Integer(4), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.registerOutParameter(3, Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(3);
		ctrlCallable.setReturnValue(new Integer(5));
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + AddInvoice.SQL + "(?, ?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		testAddInvoiceUsingObjectArray(1106, 4);

	}

	public void testAddInvoicesWithinTransaction() throws Exception {
		mockCallable.setObject(1, new Integer(1106), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.setObject(2, new Integer(3), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.registerOutParameter(3, Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(3);
		ctrlCallable.setReturnValue(new Integer(4));
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + AddInvoice.SQL + "(?, ?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();

		TransactionSynchronizationManager.bindResource(
			mockDataSource,
			new ConnectionHolder(mockConnection));

		try {
			testAddInvoice(1106, 3);
		}
		finally {
			TransactionSynchronizationManager.unbindResource(mockDataSource);
		}
	}


	/**
	 * Confirm no connection was used to get metadata.
	 * Does not use superclass replay mechanism.
	 * @throws Exception
	 */
	public void testStoredProcedureConfiguredViaJdbcTemplateWithCustomExceptionTranslator() throws Exception {
		mockCallable.setObject(1, new Integer(11), Types.INTEGER);
		ctrlCallable.setVoidCallable(1);
		mockCallable.registerOutParameter(2, Types.INTEGER);
		ctrlCallable.setVoidCallable(1);
		mockCallable.execute();
		ctrlCallable.setReturnValue(false, 1);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(2);
		ctrlCallable.setReturnValue(new Integer(5), 1);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable(1);
		// Must call this here as we're not using setUp()/tearDown() mechanism
		ctrlCallable.replay();

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.prepareCall("{call " + StoredProcedureConfiguredViaJdbcTemplate.SQL + "(?, ?)}");
		ctrlConnection.setReturnValue(mockCallable, 1);
		mockConnection.close();
		ctrlConnection.setVoidCallable(1);
		ctrlConnection.replay();

		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource localDs = (DataSource) dsControl.getMock();
		localDs.getConnection();
		dsControl.setReturnValue(mockConnection, 1);
		dsControl.replay();

		class TestJdbcTemplate extends JdbcTemplate {
			int calls;
			public Map call(CallableStatementCreator csc, List declaredParameters) throws DataAccessException {
				calls++;
				return super.call(csc, declaredParameters);
			}

		}
		TestJdbcTemplate t = new TestJdbcTemplate();
		t.setDataSource(localDs);
		// Will fail without the following, because we're not able to get a connection from the
		// DataSource here if we need to to create an ExceptionTranslator
		t.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		StoredProcedureConfiguredViaJdbcTemplate sp = new StoredProcedureConfiguredViaJdbcTemplate(t);

		assertEquals(sp.execute(11), 5);
		assertEquals(1, t.calls);

		dsControl.verify();
		ctrlCallable.verify();
		ctrlConnection.verify();
	}

	/**
	 * Confirm our JdbcTemplate is used
	 * @throws Exception
	 */
	public void testStoredProcedureConfiguredViaJdbcTemplate() throws Exception {
		mockCallable.setObject(1, new Integer(1106), Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.registerOutParameter(2, Types.INTEGER);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(2);
		ctrlCallable.setReturnValue(new Integer(4));
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + StoredProcedureConfiguredViaJdbcTemplate.SQL + "(?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		JdbcTemplate t = new JdbcTemplate();
		t.setDataSource(mockDataSource);
		StoredProcedureConfiguredViaJdbcTemplate sp = new StoredProcedureConfiguredViaJdbcTemplate(t);

		assertEquals(sp.execute(1106), 4);
	}

	public void testNullArg() throws Exception {
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);

		mockCallable.setNull(1, Types.VARCHAR);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + NullArg.SQL + "(?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		ctrlResultSet.replay();

		NullArg na = new NullArg(mockDataSource);
		na.execute((String) null);
	}

	public void testUnnamedParameter() throws Exception {
		replay();
		try {
			UnnamedParameterStoredProcedure unp =
				new UnnamedParameterStoredProcedure(mockDataSource);
			fail("Shouldn't succeed in creating stored procedure with unnamed parameter");
		} catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testMissingParameter() throws Exception {
		replay();

		try {
			MissingParameterStoredProcedure mp =
				new MissingParameterStoredProcedure(mockDataSource);
			mp.execute();
			fail("Shouldn't succeed in running stored procedure with missing required parameter");
		} catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testStoredProcedureExceptionTranslator() throws Exception {
		SQLException sex =
			new SQLException(
				"Syntax error or access violation exception",
				"42000");
		mockCallable.execute();
		ctrlCallable.setThrowable(sex);
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall(
			"{call " + StoredProcedureExceptionTranslator.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();

		StoredProcedureExceptionTranslator sproc =
			new StoredProcedureExceptionTranslator(mockDataSource);
		try {
			sproc.execute();
			fail("Custom exception should be thrown");
		} catch (CustomDataException ex) {
			// OK
		}
	}

	public void testStoredProcedureWithResultSet() throws Exception {
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockCallable.execute();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getResultSet();
		ctrlCallable.setReturnValue(mockResultSet);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + StoredProcedureWithResultSet.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		ctrlResultSet.replay();

		StoredProcedureWithResultSet sproc = new StoredProcedureWithResultSet(mockDataSource);
		sproc.execute();

		ctrlResultSet.verify();
		assertEquals(2, sproc.getCount());
	}

	public void testStoredProcedureWithResultSetMapped() throws Exception {
		MockControl ctrlResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(2);
		ctrlResultSet.setReturnValue("Foo");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(2);
		ctrlResultSet.setReturnValue("Bar");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockCallable.execute();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getResultSet();
		ctrlCallable.setReturnValue(mockResultSet);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		ctrlResultSet.replay();

		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(mockDataSource);
		Map res = sproc.execute();

		ctrlResultSet.verify();

		List rs = (List) res.get("rs");
		assertEquals(2, rs.size());
		assertEquals("Foo", rs.get(0));
		assertEquals("Bar", rs.get(1));

	}

	public void testStoredProcedureWithUndeclaredResults() throws Exception {
		MockControl ctrlResultSet1 = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet1 = (ResultSet) ctrlResultSet1.getMock();
		mockResultSet1.next();
		ctrlResultSet1.setReturnValue(true);
		mockResultSet1.getString(2);
		ctrlResultSet1.setReturnValue("Foo");
		mockResultSet1.next();
		ctrlResultSet1.setReturnValue(true);
		mockResultSet1.getString(2);
		ctrlResultSet1.setReturnValue("Bar");
		mockResultSet1.next();
		ctrlResultSet1.setReturnValue(false);
		mockResultSet1.close();
		ctrlResultSet1.setVoidCallable();

		MockControl ctrlMetaData = MockControl.createControl(ResultSetMetaData.class);
		ResultSetMetaData mockMetaData = (ResultSetMetaData) ctrlMetaData.getMock();
		mockMetaData.getColumnCount();
		ctrlMetaData.setReturnValue(2);
		mockMetaData.getColumnLabel(1);
		ctrlMetaData.setReturnValue("spam");
		mockMetaData.getColumnLabel(2);
		ctrlMetaData.setReturnValue("eggs");

		MockControl ctrlResultSet2 = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet2 = (ResultSet) ctrlResultSet2.getMock();
		mockResultSet2.getMetaData();
		ctrlResultSet2.setReturnValue(mockMetaData);
		mockResultSet2.next();
		ctrlResultSet2.setReturnValue(true);
		mockResultSet2.getObject(1);
		ctrlResultSet2.setReturnValue("Spam");
		mockResultSet2.getObject(2);
		ctrlResultSet2.setReturnValue("Eggs");
		mockResultSet2.next();
		ctrlResultSet2.setReturnValue(false);
		mockResultSet2.close();
		ctrlResultSet2.setVoidCallable();

		mockCallable.execute();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getResultSet();
		ctrlCallable.setReturnValue(mockResultSet1);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getResultSet();
		ctrlCallable.setReturnValue(mockResultSet2);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(0);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		ctrlResultSet1.replay();
		ctrlMetaData.replay();
		ctrlResultSet2.replay();

		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(mockDataSource);
		Map res = sproc.execute();

		ctrlResultSet1.verify();
		ctrlResultSet2.verify();

		assertEquals("incorrect number of returns", 3, res.size());

		List rs1 = (List) res.get("rs");
		assertEquals(2, rs1.size());
		assertEquals("Foo", rs1.get(0));
		assertEquals("Bar", rs1.get(1));

		List rs2 = (List) res.get("#result-set-2");
		assertEquals(1, rs2.size());
		Object o2 = rs2.get(0);
		assertTrue("wron type returned for result set 2", o2 instanceof Map);
		Map m2 = (Map) o2;
		assertEquals("Spam", m2.get("spam"));
		assertEquals("Eggs", m2.get("eggs"));

		Number n = (Number) res.get("#update-count-1");
		assertEquals("wrong update count", 0, n.intValue());
	}

	public void testStoredProcedureSkippingResultsProcessing() throws Exception {
		mockCallable.execute();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(mockDataSource);
		jdbcTemplate.setSkipResultsProcessing(true);
		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(jdbcTemplate);
		Map res = sproc.execute();

		assertEquals("incorrect number of returns", 0, res.size());
	}

	public void testStoredProcedureSkippingUndeclaredResults() throws Exception {
		MockControl ctrlResultSet1 = MockControl.createControl(ResultSet.class);
		ResultSet mockResultSet1 = (ResultSet) ctrlResultSet1.getMock();
		mockResultSet1.next();
		ctrlResultSet1.setReturnValue(true);
		mockResultSet1.getString(2);
		ctrlResultSet1.setReturnValue("Foo");
		mockResultSet1.next();
		ctrlResultSet1.setReturnValue(true);
		mockResultSet1.getString(2);
		ctrlResultSet1.setReturnValue("Bar");
		mockResultSet1.next();
		ctrlResultSet1.setReturnValue(false);
		mockResultSet1.close();
		ctrlResultSet1.setVoidCallable();

		mockCallable.execute();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getResultSet();
		ctrlCallable.setReturnValue(mockResultSet1);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(true);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getMoreResults();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall("{call " + StoredProcedureWithResultSetMapped.SQL + "()}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		ctrlResultSet1.replay();

		JdbcTemplate jdbcTemplate = new JdbcTemplate(mockDataSource);
		jdbcTemplate.setSkipUndeclaredResults(true);
		StoredProcedureWithResultSetMapped sproc = new StoredProcedureWithResultSetMapped(jdbcTemplate);
		Map res = sproc.execute();

		ctrlResultSet1.verify();

		assertEquals("incorrect number of returns", 1, res.size());

		List rs1 = (List) res.get("rs");
		assertEquals(2, rs1.size());
		assertEquals("Foo", rs1.get(0));
		assertEquals("Bar", rs1.get(1));
	}

	public void testParameterMapper() throws Exception {
		mockCallable.setString(1, "EasyMock for interface java.sql.Connection");
		ctrlCallable.setVoidCallable();
		mockCallable.registerOutParameter(2, Types.VARCHAR);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(2);
		ctrlCallable.setReturnValue("OK");
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall(
			"{call " + ParameterMapperStoredProcedure.SQL + "(?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		ParameterMapperStoredProcedure pmsp = new ParameterMapperStoredProcedure(mockDataSource);
		Map out = pmsp.executeTest();
		assertEquals("OK", out.get("out"));
	}

	public void testSqlTypeValue() throws Exception {
		int[] testVal = new int[] {1, 2};
		mockCallable.getConnection();
		ctrlCallable.setDefaultReturnValue(mockConnection);
		mockCallable.setObject(1, testVal, Types.ARRAY);
		ctrlCallable.setVoidCallable();
		mockCallable.registerOutParameter(2, Types.VARCHAR);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(2);
		ctrlCallable.setReturnValue("OK");
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall(
			"{call " + SqlTypeValueStoredProcedure.SQL + "(?, ?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		SqlTypeValueStoredProcedure stvsp = new SqlTypeValueStoredProcedure(mockDataSource);
		Map out = stvsp.executeTest(testVal);
		assertEquals("OK", out.get("out"));
	}

	public void testNumericWithScale() throws Exception {
		mockCallable.getConnection();
		ctrlCallable.setDefaultReturnValue(mockConnection);
		mockCallable.registerOutParameter(1, Types.DECIMAL, 4);
		ctrlCallable.setVoidCallable();
		mockCallable.execute();
		ctrlCallable.setReturnValue(false);
		mockCallable.getUpdateCount();
		ctrlCallable.setReturnValue(-1);
		mockCallable.getObject(1);
		ctrlCallable.setReturnValue(new BigDecimal("12345.6789"));
		if (debugEnabled) {
			mockCallable.getWarnings();
			ctrlCallable.setReturnValue(null);
		}
		mockCallable.close();
		ctrlCallable.setVoidCallable();

		mockConnection.prepareCall(
			"{call " + NumericWithScaleStoredProcedure.SQL + "(?)}");
		ctrlConnection.setReturnValue(mockCallable);

		replay();
		NumericWithScaleStoredProcedure nwssp = new NumericWithScaleStoredProcedure(mockDataSource);
		Map out = nwssp.executeTest();
		assertEquals(new BigDecimal("12345.6789"), out.get("out"));
	}


	private static class StoredProcedureConfiguredViaJdbcTemplate extends StoredProcedure {

		public static final String SQL = "configured_via_jt";

		public StoredProcedureConfiguredViaJdbcTemplate(JdbcTemplate t) {
			setJdbcTemplate(t);
			setSql(SQL);
			declareParameter(new SqlParameter("intIn", Types.INTEGER));
			declareParameter(new SqlOutParameter("intOut", Types.INTEGER));
			compile();
		}

		public int execute(int intIn) {
			Map in = new HashMap();
			in.put("intIn", new Integer(intIn));
			Map out = execute(in);
			Number intOut = (Number) out.get("intOut");
			return intOut.intValue();
		}
	}


	private static class AddInvoice extends StoredProcedure {

		public static final String SQL = "add_invoice";

		public AddInvoice(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("amount", Types.INTEGER));
			declareParameter(new SqlParameter("custid", Types.INTEGER));
			declareParameter(new SqlOutParameter("newid", Types.INTEGER));
			compile();
		}

		public int execute(int amount, int custid) {
			Map in = new HashMap();
			in.put("amount", new Integer(amount));
			in.put("custid", new Integer(custid));
			Map out = execute(in);
			Number id = (Number) out.get("newid");
			return id.intValue();
		}
	}

	private static class AddInvoiceUsingObjectArray extends StoredProcedure {

		public static final String SQL = "add_invoice";

		public AddInvoiceUsingObjectArray(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("amount", Types.INTEGER));
			declareParameter(new SqlParameter("custid", Types.INTEGER));
			declareParameter(new SqlOutParameter("newid", Types.INTEGER));
			compile();
		}

		public int execute(int amount, int custid) {
			Map out = execute(new Object[] {amount, custid});
			System.out.println("####### " + out);
			Number id = (Number) out.get("newid");
			return id.intValue();
		}
	}


	private static class NullArg extends StoredProcedure {

		public static final String SQL = "takes_null";

		public NullArg(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("ptest", Types.VARCHAR));
			compile();
		}

		public void execute(String s) {
			Map in = new HashMap();
			in.put("ptest", s);
			Map out = execute(in);
		}
	}


	private static class NoSuchStoredProcedure extends StoredProcedure {

		public static final String SQL = "no_sproc_with_this_name";

		public NoSuchStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			compile();
		}

		public void execute() {
			execute(new HashMap());
		}
	}


	private static class UnnamedParameterStoredProcedure extends StoredProcedure {

		public UnnamedParameterStoredProcedure(DataSource ds) {
			super(ds, "unnamed_parameter_sp");
			declareParameter(new SqlParameter(Types.INTEGER));
			compile();
		}

		public void execute(int id) {
			Map in = new HashMap();
			in.put("id", new Integer(id));
			Map out = execute(in);
		}
	}


	private static class MissingParameterStoredProcedure extends StoredProcedure {

		public MissingParameterStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql("takes_string");
			declareParameter(new SqlParameter("mystring", Types.VARCHAR));
			compile();
		}

		public void execute() {
			execute(new HashMap());
		}
	}


	private static class StoredProcedureWithResultSet extends StoredProcedure {

		public static final String SQL = "sproc_with_result_set";

		private final SimpleRowCountCallbackHandler handler = new SimpleRowCountCallbackHandler();

		public StoredProcedureWithResultSet(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlReturnResultSet("rs", this.handler));
			compile();
		}

		public void execute() {
			execute(new HashMap());
		}

		public int getCount() {
			return this.handler.getCount();
		}
	}


	private static class StoredProcedureWithResultSetMapped extends StoredProcedure {

		public static final String SQL = "sproc_with_result_set";

		public StoredProcedureWithResultSetMapped(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(
				new SqlReturnResultSet("rs", new RowMapperImpl()));
			compile();
		}

		public StoredProcedureWithResultSetMapped(JdbcTemplate jt) {
			setJdbcTemplate(jt);
			setSql(SQL);
			declareParameter(
				new SqlReturnResultSet("rs", new RowMapperImpl()));
			compile();
		}

		public Map execute() {
			Map out = execute(new HashMap());
			return out;
		}

		private static class RowMapperImpl implements RowMapper {
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
				return rs.getString(2);
			}
		}
	}


	private static class ParameterMapperStoredProcedure extends StoredProcedure {

		public static final String SQL = "parameter_mapper_sp";

		public ParameterMapperStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("in", Types.VARCHAR));
			declareParameter(new SqlOutParameter("out", Types.VARCHAR));
			compile();
		}

		public Map executeTest() {
			Map out = null;
			out = execute(new TestParameterMapper());
			return out;
		}

		private static class TestParameterMapper implements ParameterMapper {

			private TestParameterMapper() {
			}

			public Map createMap(Connection conn) throws SQLException {
				Map inParms = new HashMap();
				String testValue = conn.toString();
				inParms.put("in", testValue);
				return inParms;
			}
		}
	}


	private static class SqlTypeValueStoredProcedure extends StoredProcedure {

		public static final String SQL = "sql_type_value_sp";

		public SqlTypeValueStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlParameter("in", Types.ARRAY, "NUMBERS"));
			declareParameter(new SqlOutParameter("out", Types.VARCHAR));
			compile();
		}

		public Map executeTest(final int[] inValue) {
			Map in = new HashMap(1);
			in.put("in", new AbstractSqlTypeValue() {
				public Object createTypeValue(Connection con, int type, String typeName) {
					//assertEquals(Connection.class, con.getClass());
					//assertEquals(Types.ARRAY, type);
					//assertEquals("NUMBER", typeName);
					return inValue;
				}
			});
			Map out = null;
			out = execute(in);
			return out;
		}
	}

	private static class NumericWithScaleStoredProcedure extends StoredProcedure {

		public static final String SQL = "numeric_with_scale_sp";

		public NumericWithScaleStoredProcedure(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			declareParameter(new SqlOutParameter("out", Types.DECIMAL, 4));
			compile();
		}

		public Map executeTest() {
			Map in = new HashMap(1);
			Map out = null;
			out = execute(in);
			return out;
		}
	}


	private static class StoredProcedureExceptionTranslator extends StoredProcedure {

		public static final String SQL = "no_sproc_with_this_name";

		public StoredProcedureExceptionTranslator(DataSource ds) {
			setDataSource(ds);
			setSql(SQL);
			getJdbcTemplate().setExceptionTranslator(new SQLExceptionTranslator() {
				public DataAccessException translate(
					String task,
					String sql,
					SQLException sqlex) {
					return new CustomDataException(sql, sqlex);
				}

			});
			compile();
		}

		public void execute() {
			execute(new HashMap());
		}
	}


	private static class CustomDataException extends DataAccessException {

		public CustomDataException(String s) {
			super(s);
		}

		public CustomDataException(String s, Throwable ex) {
			super(s, ex);
		}
	}

}
