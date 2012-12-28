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

package org.springframework.jdbc.core;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import junit.framework.TestCase;
import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.Person;
import org.springframework.jdbc.core.test.SpacePerson;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/**
 * Mock object based abstract class for RowMapper tests.
 * Initializes mock objects and verifies results.
 *
 * @author Thomas Risberg
 */
public abstract class AbstractRowMapperTests extends TestCase {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	protected MockControl conControl;
	protected Connection con;
	protected MockControl conControl2;
	protected Connection con2;
	protected MockControl conControl3;
	protected Connection con3;

	protected MockControl rsmdControl;
	protected ResultSetMetaData rsmd;
	protected MockControl rsControl;
	protected ResultSet rs;
	protected MockControl stmtControl;
	protected Statement stmt;
	protected JdbcTemplate jdbcTemplate;

	protected MockControl rsmdControl2;
	protected ResultSetMetaData rsmd2;
	protected MockControl rsControl2;
	protected ResultSet rs2;
	protected MockControl stmtControl2;
	protected Statement stmt2;
	protected JdbcTemplate jdbcTemplate2;

	protected MockControl rsmdControl3;
	protected ResultSetMetaData rsmd3;
	protected MockControl rsControl3;
	protected ResultSet rs3;
	protected MockControl stmtControl3;
	protected Statement stmt3;
	protected JdbcTemplate jdbcTemplate3;

	@Override
	protected void setUp() throws SQLException {
		conControl = MockControl.createControl(Connection.class);
		con = (Connection) conControl.getMock();
		con.isClosed();
		conControl.setDefaultReturnValue(false);

		rsmdControl = MockControl.createControl(ResultSetMetaData.class);
		rsmd = (ResultSetMetaData)rsmdControl.getMock();
		rsmd.getColumnCount();
		rsmdControl.setReturnValue(4, 1);
		rsmd.getColumnLabel(1);
		rsmdControl.setReturnValue("name", 1);
		rsmd.getColumnLabel(2);
		rsmdControl.setReturnValue("age", 1);
		rsmd.getColumnLabel(3);
		rsmdControl.setReturnValue("birth_date", 1);
		rsmd.getColumnLabel(4);
		rsmdControl.setReturnValue("balance", 1);
		rsmdControl.replay();

		rsControl = MockControl.createControl(ResultSet.class);
		rs = (ResultSet) rsControl.getMock();
		rs.getMetaData();
		rsControl.setReturnValue(rsmd, 1);
		rs.next();
		rsControl.setReturnValue(true, 1);
		rs.getString(1);
		rsControl.setReturnValue("Bubba", 1);
		rs.wasNull();
		rsControl.setReturnValue(false, 1);
		rs.getLong(2);
		rsControl.setReturnValue(22, 1);
		rs.getTimestamp(3);
		rsControl.setReturnValue(new Timestamp(1221222L), 1);
		rs.getBigDecimal(4);
		rsControl.setReturnValue(new BigDecimal("1234.56"), 1);
		rs.next();
		rsControl.setReturnValue(false, 1);
		rs.close();
		rsControl.setVoidCallable(1);
		rsControl.replay();

		stmtControl = MockControl.createControl(Statement.class);
		stmt = (Statement) stmtControl.getMock();

		con.createStatement();
		conControl.setReturnValue(stmt, 1);
		stmt.executeQuery("select name, age, birth_date, balance from people");
		stmtControl.setReturnValue(rs, 1);
		if (debugEnabled) {
			stmt.getWarnings();
			stmtControl.setReturnValue(null, 1);
		}
		stmt.close();
		stmtControl.setVoidCallable(1);

		conControl.replay();
		stmtControl.replay();

		conControl2 = MockControl.createControl(Connection.class);
		con2 = (Connection) conControl2.getMock();
		con2.isClosed();
		conControl2.setDefaultReturnValue(false);

		rsmdControl2 = MockControl.createControl(ResultSetMetaData.class);
		rsmd2 = (ResultSetMetaData)rsmdControl2.getMock();
		rsmd2.getColumnCount();
		rsmdControl2.setReturnValue(4, 2);
		rsmd2.getColumnLabel(1);
		rsmdControl2.setReturnValue("name", 2);
		rsmd2.getColumnLabel(2);
		rsmdControl2.setReturnValue("age", 2);
		rsmd2.getColumnLabel(3);
		rsmdControl2.setReturnValue("birth_date", 1);
		rsmd2.getColumnLabel(4);
		rsmdControl2.setReturnValue("balance", 1);
		rsmdControl2.replay();

		rsControl2 = MockControl.createControl(ResultSet.class);
		rs2 = (ResultSet) rsControl2.getMock();
		rs2.getMetaData();
		rsControl2.setReturnValue(rsmd2, 2);
		rs2.next();
		rsControl2.setReturnValue(true, 2);
		rs2.getString(1);
		rsControl2.setReturnValue("Bubba", 2);
		rs2.wasNull();
		rsControl2.setReturnValue(true, 2);
		rs2.getLong(2);
		rsControl2.setReturnValue(0, 2);
		rs2.getTimestamp(3);
		rsControl2.setReturnValue(new Timestamp(1221222L), 1);
		rs2.getBigDecimal(4);
		rsControl2.setReturnValue(new BigDecimal("1234.56"), 1);
		rs2.next();
		rsControl2.setReturnValue(false, 1);
		rs2.close();
		rsControl2.setVoidCallable(2);
		rsControl2.replay();

		stmtControl2 = MockControl.createControl(Statement.class);
		stmt2 = (Statement) stmtControl2.getMock();

		con2.createStatement();
		conControl2.setReturnValue(stmt2, 2);
		stmt2.executeQuery("select name, null as age, birth_date, balance from people");
		stmtControl2.setReturnValue(rs2, 2);
		if (debugEnabled) {
			stmt2.getWarnings();
			stmtControl2.setReturnValue(null, 2);
		}
		stmt2.close();
		stmtControl2.setVoidCallable(2);

		conControl2.replay();
		stmtControl2.replay();

		conControl3 = MockControl.createControl(Connection.class);
		con3 = (Connection) conControl3.getMock();
		con3.isClosed();
		conControl3.setDefaultReturnValue(false);

		rsmdControl3 = MockControl.createControl(ResultSetMetaData.class);
		rsmd3 = (ResultSetMetaData)rsmdControl3.getMock();
		rsmd3.getColumnCount();
		rsmdControl3.setReturnValue(4, 1);
		rsmd3.getColumnLabel(1);
		rsmdControl3.setReturnValue("Last Name", 1);
		rsmd3.getColumnLabel(2);
		rsmdControl3.setReturnValue("age", 1);
		rsmd3.getColumnLabel(3);
		rsmdControl3.setReturnValue("birth_date", 1);
		rsmd3.getColumnLabel(4);
		rsmdControl3.setReturnValue("balance", 1);
		rsmdControl3.replay();

		rsControl3 = MockControl.createControl(ResultSet.class);
		rs3 = (ResultSet) rsControl3.getMock();
		rs3.getMetaData();
		rsControl3.setReturnValue(rsmd3, 1);
		rs3.next();
		rsControl3.setReturnValue(true, 1);
		rs3.getString(1);
		rsControl3.setReturnValue("Gagarin", 1);
		rs3.wasNull();
		rsControl3.setReturnValue(false, 1);
		rs3.getLong(2);
		rsControl3.setReturnValue(22, 1);
		rs3.getTimestamp(3);
		rsControl3.setReturnValue(new Timestamp(1221222L), 1);
		rs3.getBigDecimal(4);
		rsControl3.setReturnValue(new BigDecimal("1234.56"), 1);
		rs3.next();
		rsControl3.setReturnValue(false, 1);
		rs3.close();
		rsControl3.setVoidCallable(1);
		rsControl3.replay();

		stmtControl3 = MockControl.createControl(Statement.class);
		stmt3 = (Statement) stmtControl3.getMock();

		con3.createStatement();
		conControl3.setReturnValue(stmt3, 1);
		stmt3.executeQuery("select last_name as \"Last Name\", age, birth_date, balance from people");
		stmtControl3.setReturnValue(rs3, 1);
		if (debugEnabled) {
			stmt3.getWarnings();
			stmtControl3.setReturnValue(null, 1);
		}
		stmt3.close();
		stmtControl3.setVoidCallable(1);

		conControl3.replay();
		stmtControl3.replay();

		jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(new SingleConnectionDataSource(con, false));
		jdbcTemplate.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		jdbcTemplate.afterPropertiesSet();

		jdbcTemplate2 = new JdbcTemplate();
		jdbcTemplate2.setDataSource(new SingleConnectionDataSource(con2, false));
		jdbcTemplate2.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		jdbcTemplate2.afterPropertiesSet();

		jdbcTemplate3 = new JdbcTemplate();
		jdbcTemplate3.setDataSource(new SingleConnectionDataSource(con3, false));
		jdbcTemplate3.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		jdbcTemplate3.afterPropertiesSet();
	}

	protected void verifyPerson(Person bean) {
		verify();
		assertEquals("Bubba", bean.getName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.util.Date(1221222L), bean.getBirth_date());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	protected void verifyPersonWithZeroAge(Person bean) {
		conControl2.verify();
		rsControl2.verify();
		rsmdControl2.verify();
		stmtControl2.verify();
		assertEquals("Bubba", bean.getName());
		assertEquals(0L, bean.getAge());
		assertEquals(new java.util.Date(1221222L), bean.getBirth_date());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	protected void verifyConcretePerson(ConcretePerson bean) {
		verify();
		assertEquals("Bubba", bean.getName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.util.Date(1221222L), bean.getBirth_date());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	protected void verifySpacePerson(SpacePerson bean) {
		conControl3.verify();
		rsControl3.verify();
		rsmdControl3.verify();
		stmtControl3.verify();
		assertEquals("Gagarin", bean.getLastName());
		assertEquals(22L, bean.getAge());
		assertEquals(new java.util.Date(1221222L), bean.getBirthDate());
		assertEquals(new BigDecimal("1234.56"), bean.getBalance());
	}

	private void verify() {
		conControl.verify();
		rsControl.verify();
		rsmdControl.verify();
		stmtControl.verify();
	}

}
