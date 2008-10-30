/*
 * Copyright 2002-2006 the original author or authors.
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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor;

/**
 * @author Andre Biryukov
 * @author Juergen Hoeller
 */
public class NativeJdbcExtractorTests extends TestCase {

	public void testSimpleNativeJdbcExtractor() throws SQLException {
		SimpleNativeJdbcExtractor extractor = new SimpleNativeJdbcExtractor();

		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl dbmdControl = MockControl.createControl(DatabaseMetaData.class);
		DatabaseMetaData dbmd = (DatabaseMetaData) dbmdControl.getMock();
		MockControl con2Control = MockControl.createControl(Connection.class);
		Connection con2 = (Connection) con2Control.getMock();
		con.getMetaData();
		conControl.setReturnValue(dbmd, 2);
		dbmd.getConnection();
		dbmdControl.setReturnValue(con2, 2);
		conControl.replay();
		dbmdControl.replay();
		con2Control.replay();

		Connection nativeCon = extractor.getNativeConnection(con);
		assertEquals(con2, nativeCon);

		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();
		stmt.getConnection();
		stmtControl.setReturnValue(con);
		stmtControl.replay();

		nativeCon = extractor.getNativeConnectionFromStatement(stmt);
		assertEquals(con2, nativeCon);

		Statement nativeStmt = extractor.getNativeStatement(stmt);
		assertEquals(nativeStmt, stmt);

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		psControl.replay();

		PreparedStatement nativePs = extractor.getNativePreparedStatement(ps);
		assertEquals(ps, nativePs);

		MockControl csControl = MockControl.createControl(CallableStatement.class);
		CallableStatement cs = (CallableStatement) csControl.getMock();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		cs.getResultSet();
		csControl.setReturnValue(rs);
		csControl.replay();
		rsControl.replay();

		CallableStatement nativeCs = extractor.getNativeCallableStatement(cs);
		assertEquals(cs, nativeCs);

		ResultSet nativeRs = extractor.getNativeResultSet(cs.getResultSet());
		assertEquals(nativeRs, rs);

		conControl.verify();
		dbmdControl.verify();
		con2Control.verify();
		stmtControl.verify();
		psControl.verify();
		csControl.verify();
		rsControl.verify();
	}

	public void testCommonsDbcpNativeJdbcExtractor() throws SQLException {
		CommonsDbcpNativeJdbcExtractor extractor = new CommonsDbcpNativeJdbcExtractor();
		assertFalse(extractor.isNativeConnectionNecessaryForNativeStatements());

		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl stmtControl = MockControl.createControl(Statement.class);
		Statement stmt = (Statement) stmtControl.getMock();
		con.getMetaData();
		conControl.setReturnValue(null, 2);
		stmt.getConnection();
		stmtControl.setReturnValue(con, 1);
		conControl.replay();
		stmtControl.replay();

		Connection nativeConnection = extractor.getNativeConnection(con);
		assertEquals(con, nativeConnection);

		nativeConnection = extractor.getNativeConnectionFromStatement(stmt);
		assertEquals(con, nativeConnection);
		assertEquals(stmt, extractor.getNativeStatement(stmt));

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		psControl.replay();
		assertEquals(ps, extractor.getNativePreparedStatement(ps));

		MockControl csControl = MockControl.createControl(CallableStatement.class);
		CallableStatement cs = (CallableStatement) csControl.getMock();
		csControl.replay();
		assertEquals(cs, extractor.getNativePreparedStatement(cs));

		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		rsControl.replay();
		assertEquals(rs, extractor.getNativeResultSet(rs));

		conControl.verify();
		stmtControl.verify();
		psControl.verify();
	}

}
