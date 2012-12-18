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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;

import junit.framework.Assert;

/**
 * @author Trevor Cook
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlQueryTests extends AbstractJdbcTests {

	private static final String SELECT_ID = "select id from custmr";
	private static final String SELECT_ID_WHERE =
		"select id from custmr where forename = ? and id = ?";
	private static final String SELECT_FORENAME = "select forename from custmr";
	private static final String SELECT_FORENAME_EMPTY =
		"select forename from custmr WHERE 1 = 2";
	private static final String SELECT_ID_FORENAME_WHERE =
		"select id, forename from prefix:custmr where forename = ?";
	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS =
		"select id, forename from custmr where id = :id and country = :country";
	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED =
		"select id, forename from custmr where id = ? and country = ?";
	private static final String SELECT_ID_FORENAME_WHERE_ID_IN_LIST_1 =
		"select id, forename from custmr where id in (?, ?)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_IN_LIST_2 =
		"select id, forename from custmr where id in (:ids)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_REUSED_1 =
		"select id, forename from custmr where id = ? or id = ?)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_REUSED_2 =
		"select id, forename from custmr where id = :id1 or id = :id1)";
	private static final String SELECT_ID_FORENAME_WHERE_ID =
		"select id, forename from custmr where id <= ?";

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};
	private static final int[] COLUMN_TYPES = new int[] {Types.INTEGER, Types.VARCHAR};

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	private MockControl ctrlPreparedStatement;
	private PreparedStatement mockPreparedStatement;
	private MockControl ctrlResultSet;
	private ResultSet mockResultSet;


	protected void setUp() throws Exception {
		super.setUp();
		ctrlPreparedStatement =	MockControl.createControl(PreparedStatement.class);
		mockPreparedStatement =	(PreparedStatement) ctrlPreparedStatement.getMock();
		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		if (shouldVerify()) {
			ctrlPreparedStatement.verify();
			ctrlResultSet.verify();
		}
	}

	protected void replay() {
		super.replay();
		ctrlPreparedStatement.replay();
		ctrlResultSet.replay();
	}


	public void testQueryWithoutParams() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt(1);
		ctrlResultSet.setReturnValue(1);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		SqlQuery query = new MappingSqlQueryWithParameters() {
			protected Object mapRow(ResultSet rs, int rownum, Object[] params, Map context) throws SQLException {
				assertTrue("params were null", params == null);
				assertTrue("context was null", context == null);
				return new Integer(rs.getInt(1));
			}
		};

		query.setDataSource(mockDataSource);
		query.setSql(SELECT_ID);
		query.compile();
		List list = query.execute();
		assertTrue("Found customers", list.size() != 0);
		for (Iterator itr = list.iterator(); itr.hasNext();) {
			Integer id = (Integer) itr.next();
			assertTrue(
				"Customer id was assigned correctly",
				id.intValue() == 1);
		}
	}

	public void testQueryWithoutEnoughParams() {
		replay();

		MappingSqlQuery query = new MappingSqlQuery() {
			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				return new Integer(rs.getInt(1));
			}

		};
		query.setDataSource(mockDataSource);
		query.setSql(SELECT_ID_WHERE);
		query.declareParameter(
			new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
		query.declareParameter(
			new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
		query.compile();

		try {
			List list = query.execute();
			fail("Shouldn't succeed in running query without enough params");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// OK
		}
	}

	public void testQueryWithMissingMapParams() {
		replay();

		MappingSqlQuery query = new MappingSqlQuery() {
			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				return new Integer(rs.getInt(1));
			}
		};
		query.setDataSource(mockDataSource);
		query.setSql(SELECT_ID_WHERE);
		query.declareParameter(
			new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
		query.declareParameter(
			new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
		query.compile();

		try {
			Map params = new HashMap();
			params.put(COLUMN_NAMES[0], "Value");
			List list = query.executeByNamedParam(params);
			fail("Shouldn't succeed in running query with missing params");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// OK
		}
	}

	public void testStringQueryWithResults() throws Exception {
		String[] dbResults = new String[] { "alpha", "beta", "charlie" };

		MockControl[] ctrlCountResultSetMetaData = new MockControl[3];
		ResultSetMetaData[] mockCountResultSetMetaData = new ResultSetMetaData[3];
		MockControl[] ctrlCountResultSet = new MockControl[3];
		ResultSet[] mockCountResultSet = new ResultSet[3];
		MockControl[] ctrlCountPreparedStatement = new MockControl[3];
		PreparedStatement[] mockCountPreparedStatement = new PreparedStatement[3];

		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(dbResults[0]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(dbResults[1]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getString(1);
		ctrlResultSet.setReturnValue(dbResults[2]);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_FORENAME);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		for (int i = 0; i < dbResults.length; i++) {
			ctrlCountResultSetMetaData[i] = MockControl.createControl(ResultSetMetaData.class);
			mockCountResultSetMetaData[i] = (ResultSetMetaData) ctrlCountResultSetMetaData[i].getMock();
			mockCountResultSetMetaData[i].getColumnCount();
			ctrlCountResultSetMetaData[i].setReturnValue(1);

			ctrlCountResultSet[i] = MockControl.createControl(ResultSet.class);
			mockCountResultSet[i] = (ResultSet) ctrlCountResultSet[i].getMock();
			mockCountResultSet[i].getMetaData();
			ctrlCountResultSet[i].setReturnValue(mockCountResultSetMetaData[i]);
			mockCountResultSet[i].next();
			ctrlCountResultSet[i].setReturnValue(true);
			mockCountResultSet[i].getInt(1);
			ctrlCountResultSet[i].setReturnValue(1);
			mockCountResultSet[i].wasNull();
			ctrlCountResultSet[i].setReturnValue(false);
			mockCountResultSet[i].next();
			ctrlCountResultSet[i].setReturnValue(false);
			mockCountResultSet[i].close();
			ctrlCountResultSet[i].setVoidCallable();

			ctrlCountPreparedStatement[i] = MockControl.createControl(PreparedStatement.class);
			mockCountPreparedStatement[i] = (PreparedStatement) ctrlCountPreparedStatement[i].getMock();
			mockCountPreparedStatement[i].executeQuery();
			ctrlCountPreparedStatement[i].setReturnValue(mockCountResultSet[i]);
			if (debugEnabled) {
				mockCountPreparedStatement[i].getWarnings();
				ctrlCountPreparedStatement[i].setReturnValue(null);
			}
			mockCountPreparedStatement[i].close();
			ctrlCountPreparedStatement[i].setVoidCallable();

			mockConnection.prepareStatement(
				"SELECT COUNT(FORENAME) FROM CUSTMR WHERE FORENAME='" + dbResults[i] + "'");
			ctrlConnection.setReturnValue(mockCountPreparedStatement[i]);

			ctrlCountResultSetMetaData[i].replay();
			ctrlCountResultSet[i].replay();
			ctrlCountPreparedStatement[i].replay();
		}

		replay();

		StringQuery query = new StringQuery(mockDataSource, SELECT_FORENAME);
		query.setRowsExpected(3);
		String[] results = query.run();
		assertTrue("Array is non null", results != null);
		assertTrue("Found results", results.length > 0);
		assertTrue(
			"Found expected number of results",
			query.getRowsExpected() == 3);

		JdbcTemplate helper = new JdbcTemplate(mockDataSource);
		for (int i = 0; i < results.length; i++) {
			// BREAKS ON ' in name
			int dbCount = helper.queryForInt(
					"SELECT COUNT(FORENAME) FROM CUSTMR WHERE FORENAME='" + results[i] + "'", (Object[]) null);
			assertTrue("found in db", dbCount == 1);
		}

		for (int i = 0; i < dbResults.length; i++) {
			ctrlCountResultSetMetaData[i].verify();
			ctrlCountResultSet[i].verify();
			ctrlCountPreparedStatement[i].verify();
		}
	}

	public void testStringQueryWithoutResults() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_FORENAME_EMPTY);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		StringQuery query =	new StringQuery(mockDataSource, SELECT_FORENAME_EMPTY);
		String[] results = query.run();
		assertTrue("Array is non null", results != null);
		assertTrue("Found 0 results", results.length == 0);
	}

	public void testFindCustomerIntInt() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(2, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, int otherNum) {
				return (Customer) findObject(id, otherNum);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		Customer cust = query.findCustomer(1, 1);

		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue(
			"Customer forename was assigned correctly",
			cust.getForename().equals("rod"));
	}

	public void testFindCustomerString() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setString(1, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID_FORENAME_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(String id) {
				return (Customer) findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		Customer cust = query.findCustomer("rod");

		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));
	}

	public void testFindCustomerMixed() throws SQLException {
		MockControl ctrlResultSet2;
		ResultSet mockResultSet2;
		MockControl ctrlPreparedStatement2;
		PreparedStatement mockPreparedStatement2;

		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.INTEGER);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(2, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		ctrlResultSet2 = MockControl.createControl(ResultSet.class);
		mockResultSet2 = (ResultSet) ctrlResultSet2.getMock();
		mockResultSet2.next();
		ctrlResultSet2.setReturnValue(false);
		mockResultSet2.close();
		ctrlResultSet2.setVoidCallable();

		ctrlPreparedStatement2 = MockControl.createControl(PreparedStatement.class);
		mockPreparedStatement2 = (PreparedStatement) ctrlPreparedStatement2.getMock();
		mockPreparedStatement2.setObject(1, new Integer(1), Types.INTEGER);
		ctrlPreparedStatement2.setVoidCallable();
		mockPreparedStatement2.setString(2, "Roger");
		ctrlPreparedStatement2.setVoidCallable();
		mockPreparedStatement2.executeQuery();
		ctrlPreparedStatement2.setReturnValue(mockResultSet2);
		if (debugEnabled) {
			mockPreparedStatement2.getWarnings();
			ctrlPreparedStatement2.setReturnValue(null);
		}
		mockPreparedStatement2.close();
		ctrlPreparedStatement2.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement);
		mockConnection.prepareStatement(SELECT_ID_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement2);

		ctrlResultSet2.replay();
		ctrlPreparedStatement2.replay();
		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(
					new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
				declareParameter(
					new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, String name) {
				return (Customer) findObject(
					new Object[] { new Integer(id), name });
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);

		Customer cust1 = query.findCustomer(1, "rod");
		assertTrue("Found customer", cust1 != null);
		assertTrue("Customer id was assigned correctly", cust1.getId() == 1);

		Customer cust2 = query.findCustomer(1, "Roger");
		assertTrue("No customer found", cust2 == null);
	}

	public void testFindTooManyCustomers() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(2);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setString(1, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID_FORENAME_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(String id) {
				return (Customer) findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		try {
			Customer cust = query.findCustomer("rod");
			fail("Should fail if more than one row found");
		}
		catch (IncorrectResultSizeDataAccessException ex) {
			// OK
		}
	}

	public void testListCustomersIntInt() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(2);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("dave");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(2, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		List list = query.execute(1, 1);
		assertTrue("2 results in list", list.size() == 2);
		for (Iterator itr = list.iterator(); itr.hasNext();) {
			Customer cust = (Customer) itr.next();
		}
	}

	public void testListCustomersString() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(2);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("dave");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setString(1, "one");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(SELECT_ID_FORENAME_WHERE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		List list = query.execute("one");
		assertTrue("2 results in list", list.size() == 2);
		for (Iterator itr = list.iterator(); itr.hasNext();) {
			Customer cust = (Customer) itr.next();
		}
	}

	public void testFancyCustomerQuery() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(
				SELECT_ID_FORENAME_WHERE, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id) {
				return (Customer) findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		Customer cust = query.findCustomer(1);
		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));
	}

	public void testUnnamedParameterDeclarationWithNamedParameterQuery() throws SQLException {
		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id) {
				Map params = new HashMap();
				params.put("id", new Integer(id));
				return (Customer) executeByNamedParam(params).get(0);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		try {
			Customer cust = query.findCustomer(1);
			fail("Query should not succeed since parameter declaration did not specify parameter name");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// OK - it worked
		}
	}

	public void testNamedParameterCustomerQueryWithUnnamedDeclarations() throws SQLException {
		doTestNamedParameterCustomerQuery(false);
	}

	public void testNamedParameterCustomerQueryWithNamedDeclarations() throws SQLException {
		doTestNamedParameterCustomerQuery(true);
	}

	private void doTestNamedParameterCustomerQuery(final boolean namedDeclarations) throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setString(2, "UK");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(
				SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_NAMED_PARAMETERS);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				if (namedDeclarations) {
					declareParameter(new SqlParameter("country", Types.VARCHAR));
					declareParameter(new SqlParameter("id", Types.NUMERIC));
				}
				else {
					declareParameter(new SqlParameter(Types.NUMERIC));
					declareParameter(new SqlParameter(Types.VARCHAR));
				}
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, String country) {
				Map params = new HashMap();
				params.put("id", new Integer(id));
				params.put("country", country);
				return (Customer) executeByNamedParam(params).get(0);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		Customer cust = query.findCustomer(1, "UK");
		assertTrue("Customer id was assigned correctly", cust.getId() == 1);
		assertTrue("Customer forename was assigned correctly", cust.getForename().equals("rod"));
	}

	public void testNamedParameterInListQuery() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(2);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("juergen");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(2, new Integer(2), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(
				SELECT_ID_FORENAME_WHERE_ID_IN_LIST_1, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_IN_LIST_2);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("ids", Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List findCustomers(List ids) {
				Map params = new HashMap();
				params.put("ids", ids);
				return (List) executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		List ids = new ArrayList();
		ids.add(new Integer(1));
		ids.add(new Integer(2));
		List cust = query.findCustomers(ids);

		assertEquals("We got two customers back", cust.size(), 2);
		Assert.assertEquals("First customer id was assigned correctly", ((Customer)cust.get(0)).getId(), 1);
		Assert.assertEquals("First customer forename was assigned correctly", ((Customer)cust.get(0)).getForename(), "rod");
		Assert.assertEquals("Second customer id was assigned correctly", ((Customer)cust.get(1)).getId(), 2);
		Assert.assertEquals("Second customer forename was assigned correctly", ((Customer)cust.get(1)).getForename(), "juergen");
	}

	public void testNamedParameterQueryReusingParameter() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("rod");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(2);
		mockResultSet.getString("forename");
		ctrlResultSet.setReturnValue("juergen");
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.setObject(2, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(
				SELECT_ID_FORENAME_WHERE_ID_REUSED_1, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_REUSED_2);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("id1", Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List findCustomers(Integer id) {
				Map params = new HashMap();
				params.put("id1", id);
				return (List) executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		List cust = query.findCustomers(new Integer(1));

		assertEquals("We got two customers back", cust.size(), 2);
		Assert.assertEquals("First customer id was assigned correctly", ((Customer)cust.get(0)).getId(), 1);
		Assert.assertEquals("First customer forename was assigned correctly", ((Customer)cust.get(0)).getForename(), "rod");
		Assert.assertEquals("Second customer id was assigned correctly", ((Customer)cust.get(1)).getId(), 2);
		Assert.assertEquals("Second customer forename was assigned correctly", ((Customer)cust.get(1)).getForename(), "juergen");
	}

	public void testNamedParameterUsingInvalidQuestionMarkPlaceHolders() throws SQLException {

		mockConnection.prepareStatement(
				SELECT_ID_FORENAME_WHERE_ID_REUSED_1, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerQuery extends MappingSqlQuery {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_REUSED_1);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("id1", Types.NUMERIC));
				compile();
			}

			protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List findCustomers(Integer id1) {
				Map params = new HashMap();
				params.put("id1", id1);
				return (List) executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(mockDataSource);
		try {
			List cust = query.findCustomers(new Integer(1));
			fail("Should have caused an InvalidDataAccessApiUsageException");
		}
		catch (InvalidDataAccessApiUsageException e){
		}

	}

	public void testUpdateCustomers() throws SQLException {
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(1);
		mockResultSet.updateString(2, "Rod");
		ctrlResultSet.setVoidCallable();
		mockResultSet.updateRow();
		ctrlResultSet.setVoidCallable();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getInt("id");
		ctrlResultSet.setReturnValue(2);
		mockResultSet.updateString(2, "Thomas");
		ctrlResultSet.setVoidCallable();
		mockResultSet.updateRow();
		ctrlResultSet.setVoidCallable();
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setObject(1, new Integer(2), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeQuery();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(
				SELECT_ID_FORENAME_WHERE_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class CustomerUpdateQuery extends UpdatableSqlQuery {

			public CustomerUpdateQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			protected Object updateRow(ResultSet rs, int rownum, Map context) throws SQLException {
				rs.updateString(2, "" + context.get(new Integer(rs.getInt(COLUMN_NAMES[0]))));
				return null;
			}
		}
		CustomerUpdateQuery query = new CustomerUpdateQuery(mockDataSource);
		Map values = new HashMap(2);
		values.put(new Integer(1), "Rod");
		values.put(new Integer(2), "Thomas");
		List customers = query.execute(2, values);
	}


	private static class StringQuery extends MappingSqlQuery {

		public StringQuery(DataSource ds, String sql) {
			super(ds, sql);
			compile();
		}

		protected Object mapRow(ResultSet rs, int rownum) throws SQLException {
			return rs.getString(1);
		}

		public String[] run() {
			List list = execute();
			String[] results = (String[]) list.toArray(new String[list.size()]);
			return results;
		}
	}

}
