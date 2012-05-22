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

package org.springframework.jdbc.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgreSQLSequenceMaxValueIncrementer;

/**
 * @author Juergen Hoeller
 * @since 27.02.2004
 */
public class DataFieldMaxValueIncrementerTests extends TestCase {

	public void testHsqlMaxValueIncrementer() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		con.createStatement();
		conControl.setReturnValue(stmt, 2);
		stmt.executeUpdate("insert into myseq values(null)");
		stmtControl.setReturnValue(1, 6);
		stmt.executeQuery("select max(identity()) from myseq");
		stmtControl.setReturnValue(rs, 6);
		rs.next();
		rsControl.setReturnValue(true, 6);
		for (long i = 0; i < 6; i++) {
			rs.getLong(1);
			rsControl.setReturnValue(i);
		}
		rs.close();
		rsControl.setVoidCallable(6);
		stmt.executeUpdate("delete from myseq where seq < 2");
		stmtControl.setReturnValue(1);
		stmt.executeUpdate("delete from myseq where seq < 5");
		stmtControl.setReturnValue(1);
		stmt.close();
		stmtControl.setVoidCallable(2);
		con.close();
		conControl.setVoidCallable(2);

		dsControl.replay();
		conControl.replay();
		stmtControl.replay();
		rsControl.replay();

		HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
		incrementer.setDataSource(ds);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.setCacheSize(3);
		incrementer.setPaddingLength(3);
		incrementer.afterPropertiesSet();

		assertEquals(0, incrementer.nextIntValue());
		assertEquals(1, incrementer.nextLongValue());
		assertEquals("002", incrementer.nextStringValue());
		assertEquals(3, incrementer.nextIntValue());
		assertEquals(4, incrementer.nextLongValue());

		dsControl.verify();
		conControl.verify();
		stmtControl.verify();
		rsControl.verify();
	}

	public void testMySQLMaxValueIncrementer() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		con.createStatement();
		conControl.setReturnValue(stmt, 2);
		stmt.executeUpdate("update myseq set seq = last_insert_id(seq + 2)");
		stmtControl.setReturnValue(1, 2);
		stmt.executeQuery("select last_insert_id()");
		stmtControl.setReturnValue(rs, 2);
		rs.next();
		rsControl.setReturnValue(true, 2);
		rs.getLong(1);
		rsControl.setReturnValue(2);
		rs.getLong(1);
		rsControl.setReturnValue(4);
		rs.close();
		rsControl.setVoidCallable(2);
		stmt.close();
		stmtControl.setVoidCallable(2);
		con.close();
		conControl.setVoidCallable(2);

		dsControl.replay();
		conControl.replay();
		stmtControl.replay();
		rsControl.replay();

		MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer();
		incrementer.setDataSource(ds);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.setCacheSize(2);
		incrementer.setPaddingLength(1);
		incrementer.afterPropertiesSet();

		assertEquals(1, incrementer.nextIntValue());
		assertEquals(2, incrementer.nextLongValue());
		assertEquals("3", incrementer.nextStringValue());
		assertEquals(4, incrementer.nextLongValue());

		dsControl.verify();
		conControl.verify();
		stmtControl.verify();
		rsControl.verify();
	}

	public void testPostgreSQLSequenceMaxValueIncrementer() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		con.createStatement();
		conControl.setReturnValue(stmt, 2);
		stmt.executeQuery("select nextval('myseq')");
		stmtControl.setReturnValue(rs, 2);
		rs.next();
		rsControl.setReturnValue(true, 2);
		rs.getLong(1);
		rsControl.setReturnValue(10);
		rs.getLong(1);
		rsControl.setReturnValue(12);
		rs.close();
		rsControl.setVoidCallable(2);
		stmt.close();
		stmtControl.setVoidCallable(2);
		con.close();
		conControl.setVoidCallable(2);

		dsControl.replay();
		conControl.replay();
		stmtControl.replay();
		rsControl.replay();

		PostgreSQLSequenceMaxValueIncrementer incrementer = new PostgreSQLSequenceMaxValueIncrementer();
		incrementer.setDataSource(ds);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(5);
		incrementer.afterPropertiesSet();

		assertEquals("00010", incrementer.nextStringValue());
		assertEquals(12, incrementer.nextIntValue());

		dsControl.verify();
		conControl.verify();
		stmtControl.verify();
		rsControl.verify();
	}

	public void testOracleSequenceMaxValueIncrementer() throws SQLException {
		MockControl dsControl = MockControl.createControl(DataSource.class);
		DataSource ds = (DataSource) dsControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();

		ds.getConnection();
		dsControl.setReturnValue(con, 2);
		con.createStatement();
		conControl.setReturnValue(stmt, 2);
		stmt.executeQuery("select myseq.nextval from dual");
		stmtControl.setReturnValue(rs, 2);
		rs.next();
		rsControl.setReturnValue(true, 2);
		rs.getLong(1);
		rsControl.setReturnValue(10);
		rs.getLong(1);
		rsControl.setReturnValue(12);
		rs.close();
		rsControl.setVoidCallable(2);
		stmt.close();
		stmtControl.setVoidCallable(2);
		con.close();
		conControl.setVoidCallable(2);

		dsControl.replay();
		conControl.replay();
		stmtControl.replay();
		rsControl.replay();

		OracleSequenceMaxValueIncrementer incrementer = new OracleSequenceMaxValueIncrementer();
		incrementer.setDataSource(ds);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(2);
		incrementer.afterPropertiesSet();

		assertEquals(10, incrementer.nextLongValue());
		assertEquals("12", incrementer.nextStringValue());

		dsControl.verify();
		conControl.verify();
		stmtControl.verify();
		rsControl.verify();
	}

}
