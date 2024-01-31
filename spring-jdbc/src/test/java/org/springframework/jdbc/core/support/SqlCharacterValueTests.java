/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.Test;

import org.springframework.jdbc.support.JdbcUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 6.1.4
 */
class SqlCharacterValueTests {

	@Test
	void withString() throws SQLException {
		String content = "abc";
		SqlCharacterValue value = new SqlCharacterValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setString(1, content);
	}

	@Test
	void withStringForClob() throws SQLException {
		String content = "abc";
		SqlCharacterValue value = new SqlCharacterValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.CLOB, null);
		verify(ps).setClob(eq(1), any(StringReader.class), eq(3L));
	}

	@Test
	void withStringForNClob() throws SQLException {
		String content = "abc";
		SqlCharacterValue value = new SqlCharacterValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.NCLOB, null);
		verify(ps).setNClob(eq(1), any(StringReader.class), eq(3L));
	}

	@Test
	void withCharArray() throws SQLException {
		char[] content = "abc".toCharArray();
		SqlCharacterValue value = new SqlCharacterValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setCharacterStream(eq(1), any(CharArrayReader.class), eq(3L));
	}

	@Test
	void withCharArrayForClob() throws SQLException {
		char[] content = "abc".toCharArray();
		SqlCharacterValue value = new SqlCharacterValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.CLOB, null);
		verify(ps).setClob(eq(1), any(CharArrayReader.class), eq(3L));
	}

	@Test
	void withCharArrayForNClob() throws SQLException {
		char[] content = "abc".toCharArray();
		SqlCharacterValue value = new SqlCharacterValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.NCLOB, null);
		verify(ps).setNClob(eq(1), any(CharArrayReader.class), eq(3L));
	}

	@Test
	void withReader() throws SQLException {
		Reader content = new StringReader("abc");
		SqlCharacterValue value = new SqlCharacterValue(content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setCharacterStream(1, content, 3L);
	}

	@Test
	void withReaderForClob() throws SQLException {
		Reader content = new StringReader("abc");
		SqlCharacterValue value = new SqlCharacterValue(content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.CLOB, null);
		verify(ps).setClob(1, content, 3L);
	}

	@Test
	void withReaderForNClob() throws SQLException {
		Reader content = new StringReader("abc");
		SqlCharacterValue value = new SqlCharacterValue(content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.NCLOB, null);
		verify(ps).setNClob(1, content, 3L);
	}

	@Test
	void withAsciiStream() throws SQLException {
		InputStream content = new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII));
		SqlCharacterValue value = new SqlCharacterValue(content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setAsciiStream(1, content, 3L);
	}

}
