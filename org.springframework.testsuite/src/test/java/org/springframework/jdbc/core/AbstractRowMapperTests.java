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
import java.sql.Types;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.core.test.ConcretePerson;
import org.springframework.jdbc.core.test.Person;
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
	protected MockControl rsmdControl;
	protected ResultSetMetaData rsmd;
	protected MockControl rsControl;
	protected ResultSet rs;
	protected MockControl stmtControl;
	protected Statement stmt;
	protected JdbcTemplate jdbcTemplate;

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

		jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(new SingleConnectionDataSource(con, false));
		jdbcTemplate.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		jdbcTemplate.afterPropertiesSet();
	}

	protected void verifyPerson(Person bean) {
		verify();
		assertEquals("Bubba", bean.getName());
		assertEquals(22L, bean.getAge());
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

	private void verify() {
		conControl.verify();
		rsControl.verify();
		rsmdControl.verify();
		stmtControl.verify();
	}

}
