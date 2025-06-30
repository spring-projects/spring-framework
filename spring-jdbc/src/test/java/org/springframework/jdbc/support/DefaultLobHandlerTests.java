/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 17.12.2003
 */
@SuppressWarnings("deprecation")
class DefaultLobHandlerTests {

	private ResultSet rs = mock();

	private PreparedStatement ps = mock();

	private LobHandler lobHandler = new DefaultLobHandler();

	private LobCreator lobCreator = lobHandler.getLobCreator();


	@Test
	void testGetBlobAsBytes() throws SQLException {
		lobHandler.getBlobAsBytes(rs, 1);
		verify(rs).getBytes(1);
	}

	@Test
	void testGetBlobAsBinaryStream() throws SQLException {
		lobHandler.getBlobAsBinaryStream(rs, 1);
		verify(rs).getBinaryStream(1);
	}

	@Test
	void testGetClobAsString() throws SQLException {
		lobHandler.getClobAsString(rs, 1);
		verify(rs).getString(1);
	}

	@Test
	void testGetClobAsAsciiStream() throws SQLException {
		lobHandler.getClobAsAsciiStream(rs, 1);
		verify(rs).getAsciiStream(1);
	}

	@Test
	void testGetClobAsCharacterStream() throws SQLException {
		lobHandler.getClobAsCharacterStream(rs, 1);
		verify(rs).getCharacterStream(1);
	}

	@Test
	void testSetBlobAsBytes() throws SQLException {
		byte[] content = "testContent".getBytes();
		lobCreator.setBlobAsBytes(ps, 1, content);
		verify(ps).setBytes(1, content);
	}

	@Test
	void testSetBlobAsBinaryStream() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setBlobAsBinaryStream(ps, 1, bis, 11);
		verify(ps).setBinaryStream(1, bis, 11);
	}

	@Test
	void testSetBlobAsBinaryStreamWithoutLength() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setBlobAsBinaryStream(ps, 1, bis, -1);
		verify(ps).setBinaryStream(1, bis);
	}

	@Test
	void testSetClobAsString() throws SQLException {
		String content = "testContent";
		lobCreator.setClobAsString(ps, 1, content);
		verify(ps).setString(1, content);
	}

	@Test
	void testSetClobAsAsciiStream() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setClobAsAsciiStream(ps, 1, bis, 11);
		verify(ps).setAsciiStream(1, bis, 11);
	}

	@Test
	void testSetClobAsAsciiStreamWithoutLength() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setClobAsAsciiStream(ps, 1, bis, -1);
		verify(ps).setAsciiStream(1, bis);
	}

	@Test
	void testSetClobAsCharacterStream() throws SQLException {
		Reader str = new StringReader("testContent");
		lobCreator.setClobAsCharacterStream(ps, 1, str, 11);
		verify(ps).setCharacterStream(1, str, 11);
	}

	@Test
	void testSetClobAsCharacterStreamWithoutLength() throws SQLException {
		Reader str = new StringReader("testContent");
		lobCreator.setClobAsCharacterStream(ps, 1, str, -1);
		verify(ps).setCharacterStream(1, str);
	}

}
