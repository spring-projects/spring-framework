/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;
import org.springframework.jdbc.support.nativejdbc.CommonsDbcpNativeJdbcExtractor;
import org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Andre Biryukov
 * @author Juergen Hoeller
 */
public class NativeJdbcExtractorTests {

	@Test
	public void testSimpleNativeJdbcExtractor() throws SQLException {
		SimpleNativeJdbcExtractor extractor = new SimpleNativeJdbcExtractor();

		Connection con = mock(Connection.class);
		DatabaseMetaData dbmd = mock(DatabaseMetaData.class);
		Connection con2 = mock(Connection.class);
		given(con.getMetaData()).willReturn(dbmd);
		given(dbmd.getConnection()).willReturn(con2);

		Connection nativeCon = extractor.getNativeConnection(con);
		assertEquals(con2, nativeCon);

		Statement stmt = mock(Statement.class);
		given(stmt.getConnection()).willReturn(con);

		nativeCon = extractor.getNativeConnectionFromStatement(stmt);
		assertEquals(con2, nativeCon);

		Statement nativeStmt = extractor.getNativeStatement(stmt);
		assertEquals(nativeStmt, stmt);

		PreparedStatement ps = mock(PreparedStatement.class);

		PreparedStatement nativePs = extractor.getNativePreparedStatement(ps);
		assertEquals(ps, nativePs);

		CallableStatement cs = mock(CallableStatement.class);
		ResultSet rs = mock(ResultSet.class);
		given(cs.getResultSet()).willReturn(rs);

		CallableStatement nativeCs = extractor.getNativeCallableStatement(cs);
		assertEquals(cs, nativeCs);

		ResultSet nativeRs = extractor.getNativeResultSet(cs.getResultSet());
		assertEquals(nativeRs, rs);
	}

	public void testCommonsDbcpNativeJdbcExtractor() throws SQLException {
		CommonsDbcpNativeJdbcExtractor extractor = new CommonsDbcpNativeJdbcExtractor();
		assertFalse(extractor.isNativeConnectionNecessaryForNativeStatements());

		Connection con = mock(Connection.class);
		Statement stmt = mock(Statement.class);
		given(stmt.getConnection()).willReturn(con);

		Connection nativeConnection = extractor.getNativeConnection(con);
		assertEquals(con, nativeConnection);

		nativeConnection = extractor.getNativeConnectionFromStatement(stmt);
		assertEquals(con, nativeConnection);
		assertEquals(stmt, extractor.getNativeStatement(stmt));

		PreparedStatement ps = mock(PreparedStatement.class);
		assertEquals(ps, extractor.getNativePreparedStatement(ps));

		CallableStatement cs = mock(CallableStatement.class);
		assertEquals(cs, extractor.getNativePreparedStatement(cs));

		ResultSet rs = mock(ResultSet.class);
		assertEquals(rs, extractor.getNativeResultSet(rs));
	}

}
