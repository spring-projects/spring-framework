/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void getBlobAsBytes() throws SQLException {
		lobHandler.getBlobAsBytes(rs, 1);
		verify(rs).getBytes(1);
	}

	@Test
	void getBlobAsBinaryStream() throws SQLException {
		lobHandler.getBlobAsBinaryStream(rs, 1);
		verify(rs).getBinaryStream(1);
	}

	@Test
	void getClobAsString() throws SQLException {
		lobHandler.getClobAsString(rs, 1);
		verify(rs).getString(1);
	}

	@Test
	void getClobAsAsciiStream() throws SQLException {
		lobHandler.getClobAsAsciiStream(rs, 1);
		verify(rs).getAsciiStream(1);
	}

	@Test
	void getClobAsCharacterStream() throws SQLException {
		lobHandler.getClobAsCharacterStream(rs, 1);
		verify(rs).getCharacterStream(1);
	}

	@Test
	void setBlobAsBytes() throws SQLException {
		byte[] content = "testContent".getBytes();
		lobCreator.setBlobAsBytes(ps, 1, content);
		verify(ps).setBytes(1, content);
	}

	@Test
	void setBlobAsBinaryStream() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setBlobAsBinaryStream(ps, 1, bis, 11);
		verify(ps).setBinaryStream(1, bis, 11);
	}

	@Test
	void setBlobAsBinaryStreamWithoutLength() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setBlobAsBinaryStream(ps, 1, bis, -1);
		verify(ps).setBinaryStream(1, bis);
	}

	@Test
	void setClobAsString() throws SQLException {
		String content = "testContent";
		lobCreator.setClobAsString(ps, 1, content);
		verify(ps).setString(1, content);
	}

	@Test
	void setClobAsAsciiStream() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setClobAsAsciiStream(ps, 1, bis, 11);
		verify(ps).setAsciiStream(1, bis, 11);
	}

	@Test
	void setClobAsAsciiStreamWithoutLength() throws SQLException {
		InputStream bis = new ByteArrayInputStream("testContent".getBytes());
		lobCreator.setClobAsAsciiStream(ps, 1, bis, -1);
		verify(ps).setAsciiStream(1, bis);
	}

	@Test
	void setClobAsCharacterStream() throws SQLException {
		Reader str = new StringReader("testContent");
		lobCreator.setClobAsCharacterStream(ps, 1, str, 11);
		verify(ps).setCharacterStream(1, str, 11);
	}

	@Test
	void setClobAsCharacterStreamWithoutLength() throws SQLException {
		Reader str = new StringReader("testContent");
		lobCreator.setClobAsCharacterStream(ps, 1, str, -1);
		verify(ps).setCharacterStream(1, str);
	}

}
