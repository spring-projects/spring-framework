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

package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.TestBean;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

/**
 * @author Juergen Hoeller
 * @since 02.08.2004
 */
public class RowMapperTests extends TestCase {

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	private MockControl conControl;
	private Connection con;
	private MockControl rsControl;
	private ResultSet rs;
	private JdbcTemplate jdbcTemplate;
	private List<TestBean> result;

	protected void setUp() throws SQLException {
		conControl = MockControl.createControl(Connection.class);
		con = (Connection) conControl.getMock();
		con.isClosed();
		conControl.setDefaultReturnValue(false);

		rsControl = MockControl.createControl(ResultSet.class);
		rs = (ResultSet) rsControl.getMock();
		rs.next();
		rsControl.setReturnValue(true, 1);
		rs.getString(1);
		rsControl.setReturnValue("tb1", 1);
		rs.getInt(2);
		rsControl.setReturnValue(1, 1);
		rs.next();
		rsControl.setReturnValue(true, 1);
		rs.getString(1);
		rsControl.setReturnValue("tb2", 1);
		rs.getInt(2);
		rsControl.setReturnValue(2, 1);
		rs.next();
		rsControl.setReturnValue(false, 1);
		rs.close();
		rsControl.setVoidCallable(1);
		rsControl.replay();

		jdbcTemplate = new JdbcTemplate();
		jdbcTemplate.setDataSource(new SingleConnectionDataSource(con, false));
		jdbcTemplate.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		jdbcTemplate.afterPropertiesSet();
	}

	public void testStaticQueryWithRowMapper() throws SQLException {
		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();

		con.createStatement();
		conControl.setReturnValue(stmt, 1);
		stmt.executeQuery("some SQL");
		stmtControl.setReturnValue(rs, 1);
		if (debugEnabled) {
			stmt.getWarnings();
			stmtControl.setReturnValue(null, 1);
		}
		stmt.close();
		stmtControl.setVoidCallable(1);

		conControl.replay();
		stmtControl.replay();

		result = jdbcTemplate.query("some SQL", new TestRowMapper());

		stmtControl.verify();
		verify();
	}

	public void testPreparedStatementCreatorWithRowMapper() throws SQLException {
		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		final PreparedStatement ps = (PreparedStatement) psControl.getMock();

		ps.executeQuery();
		psControl.setReturnValue(rs, 1);
		if (debugEnabled) {
			ps.getWarnings();
			psControl.setReturnValue(null, 1);
		}
		ps.close();
		psControl.setVoidCallable(1);

		conControl.replay();
		psControl.replay();

		result = jdbcTemplate.query(
				new PreparedStatementCreator() {
					public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
						return ps;
					}
				}, new TestRowMapper());

		psControl.verify();
		verify();
	}

	public void testPreparedStatementSetterWithRowMapper() throws SQLException {
		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		final PreparedStatement ps = (PreparedStatement) psControl.getMock();

		con.prepareStatement("some SQL");
		conControl.setReturnValue(ps, 1);
		ps.setString(1, "test");
		psControl.setVoidCallable(1);
		ps.executeQuery();
		psControl.setReturnValue(rs, 1);
		if (debugEnabled) {
			ps.getWarnings();
			psControl.setReturnValue(null, 1);
		}
		ps.close();
		psControl.setVoidCallable(1);

		conControl.replay();
		psControl.replay();

		result = jdbcTemplate.query(
				"some SQL",
				new PreparedStatementSetter() {
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setString(1, "test");
					}
				}, new TestRowMapper());

		psControl.verify();
		verify();
	}

	public void testQueryWithArgsAndRowMapper() throws SQLException {
		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		final PreparedStatement ps = (PreparedStatement) psControl.getMock();

		con.prepareStatement("some SQL");
		conControl.setReturnValue(ps, 1);
		ps.setString(1, "test1");
		ps.setString(2, "test2");
		psControl.setVoidCallable(1);
		ps.executeQuery();
		psControl.setReturnValue(rs, 1);
		if (debugEnabled) {
			ps.getWarnings();
			psControl.setReturnValue(null, 1);
		}
		ps.close();
		psControl.setVoidCallable(1);

		conControl.replay();
		psControl.replay();

		result = jdbcTemplate.query(
				"some SQL",
				new Object[] {"test1", "test2"},
				new TestRowMapper());

		psControl.verify();
		verify();
	}

	public void testQueryWithArgsAndTypesAndRowMapper() throws SQLException {
		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		final PreparedStatement ps = (PreparedStatement) psControl.getMock();

		con.prepareStatement("some SQL");
		conControl.setReturnValue(ps, 1);
		ps.setString(1, "test1");
		ps.setString(2, "test2");
		psControl.setVoidCallable(1);
		ps.executeQuery();
		psControl.setReturnValue(rs, 1);
		if (debugEnabled) {
			ps.getWarnings();
			psControl.setReturnValue(null, 1);
		}
		ps.close();
		psControl.setVoidCallable(1);

		conControl.replay();
		psControl.replay();

		result = jdbcTemplate.query(
				"some SQL",
				new Object[] {"test1", "test2"},
				new int[] {Types.VARCHAR, Types.VARCHAR},
				new TestRowMapper());

		psControl.verify();
		verify();
	}

	protected void verify() {
		conControl.verify();
		rsControl.verify();

		assertTrue(result != null);
		assertEquals(2, result.size());
		TestBean tb1 = result.get(0);
		TestBean tb2 = result.get(1);
		assertEquals("tb1", tb1.getName());
		assertEquals(1, tb1.getAge());
		assertEquals("tb2", tb2.getName());
		assertEquals(2, tb2.getAge());
	}


	private static class TestRowMapper implements RowMapper<TestBean> {

		public TestBean mapRow(ResultSet rs, int rowNum) throws SQLException {
			return new TestBean(rs.getString(1), rs.getInt(2));
		}
	}

}
