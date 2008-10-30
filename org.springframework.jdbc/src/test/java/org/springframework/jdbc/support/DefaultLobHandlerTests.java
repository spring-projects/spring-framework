/*
 * Copyright 2002-2005 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * @author Juergen Hoeller
 * @since 17.12.2003
 */
public class DefaultLobHandlerTests extends TestCase {

	public void testGetBlobAsBytes() throws SQLException {
		LobHandler lobHandler = new DefaultLobHandler();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		rs.getBytes(1);
		rsControl.setReturnValue(null);
		rsControl.replay();
		lobHandler.getBlobAsBytes(rs, 1);
		rsControl.verify();
	}

	public void testGetBlobAsBinaryStream() throws SQLException {
		LobHandler lobHandler = new DefaultLobHandler();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		rs.getBinaryStream(1);
		rsControl.setReturnValue(null);
		rsControl.replay();
		lobHandler.getBlobAsBinaryStream(rs, 1);
		rsControl.verify();
	}

	public void testGetClobAsString() throws SQLException {
		LobHandler lobHandler = new DefaultLobHandler();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		rs.getString(1);
		rsControl.setReturnValue(null);
		rsControl.replay();
		lobHandler.getClobAsString(rs, 1);
		rsControl.verify();
	}

	public void testGetClobAsAsciiStream() throws SQLException {
		LobHandler lobHandler = new DefaultLobHandler();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		rs.getAsciiStream(1);
		rsControl.setReturnValue(null);
		rsControl.replay();
		lobHandler.getClobAsAsciiStream(rs, 1);
		rsControl.verify();
	}

	public void testGetClobAsCharacterStream() throws SQLException {
		LobHandler lobHandler = new DefaultLobHandler();
		MockControl rsControl = MockControl.createControl(ResultSet.class);
		ResultSet rs = (ResultSet) rsControl.getMock();
		rs.getCharacterStream(1);
		rsControl.setReturnValue(null);
		rsControl.replay();
		lobHandler.getClobAsCharacterStream(rs, 1);
		rsControl.verify();
	}

	public void testSetBlobAsBytes() throws SQLException {
		LobCreator lobCreator = (new DefaultLobHandler()).getLobCreator();
		byte[] content = "testContent".getBytes();

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		ps.setBytes(1, content);
		psControl.replay();

		lobCreator.setBlobAsBytes(ps, 1, content);
		psControl.verify();
	}

	public void testSetBlobAsBinaryStream() throws SQLException, IOException {
		LobCreator lobCreator = (new DefaultLobHandler()).getLobCreator();
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		ps.setBinaryStream(1, bis, 11);
		psControl.replay();

		lobCreator.setBlobAsBinaryStream(ps, 1, bis, 11);
		psControl.verify();
	}

	public void testSetClobAsString() throws SQLException, IOException {
		LobCreator lobCreator = (new DefaultLobHandler()).getLobCreator();
		String content = "testContent";

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		ps.setString(1, content);
		psControl.replay();

		lobCreator.setClobAsString(ps, 1, content);
		psControl.verify();
	}

	public void testSetClobAsAsciiStream() throws SQLException, IOException {
		LobCreator lobCreator = (new DefaultLobHandler()).getLobCreator();
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		ps.setAsciiStream(1, bis, 11);
		psControl.replay();

		lobCreator.setClobAsAsciiStream(ps, 1, bis, 11);
		psControl.verify();
	}

	public void testSetClobAsCharacterStream() throws SQLException, IOException {
		LobCreator lobCreator = (new DefaultLobHandler()).getLobCreator();
		Reader str = new StringReader("testContent");

		MockControl psControl = MockControl.createControl(PreparedStatement.class);
		PreparedStatement ps = (PreparedStatement) psControl.getMock();
		ps.setCharacterStream(1, str, 11);
		psControl.replay();

		lobCreator.setClobAsCharacterStream(ps, 1, str, 11);
		psControl.verify();
	}

}
