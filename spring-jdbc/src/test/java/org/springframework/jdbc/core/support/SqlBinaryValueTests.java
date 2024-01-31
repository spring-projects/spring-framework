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
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.support.JdbcUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 6.1.4
 */
class SqlBinaryValueTests {

	@Test
	void withByteArray() throws SQLException {
		byte[] content = new byte[] {0, 1, 2};
		SqlBinaryValue value = new SqlBinaryValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setBytes(1, content);
	}

	@Test
	void withByteArrayForBlob() throws SQLException {
		byte[] content = new byte[] {0, 1, 2};
		SqlBinaryValue value = new SqlBinaryValue(content);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.BLOB, null);
		verify(ps).setBlob(eq(1), any(ByteArrayInputStream.class), eq(3L));
	}

	@Test
	void withInputStream() throws SQLException {
		InputStream content = new ByteArrayInputStream(new byte[] {0, 1, 2});
		SqlBinaryValue value = new SqlBinaryValue(content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setBinaryStream(1, content, 3L);
	}

	@Test
	void withInputStreamForBlob() throws SQLException {
		InputStream content = new ByteArrayInputStream(new byte[] {0, 1, 2});
		SqlBinaryValue value = new SqlBinaryValue(content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.BLOB, null);
		verify(ps).setBlob(1, content, 3L);
	}

	@Test
	void withInputStreamSource() throws SQLException {
		InputStream content = new ByteArrayInputStream(new byte[] {0, 1, 2});
		SqlBinaryValue value = new SqlBinaryValue(() -> content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setBinaryStream(1, content, 3L);
	}

	@Test
	void withInputStreamSourceForBlob() throws SQLException {
		InputStream content = new ByteArrayInputStream(new byte[] {0, 1, 2});
		SqlBinaryValue value = new SqlBinaryValue(() -> content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.BLOB, null);
		verify(ps).setBlob(1, content, 3L);
	}

	@Test
	void withResource() throws SQLException {
		byte[] content = new byte[] {0, 1, 2};
		SqlBinaryValue value = new SqlBinaryValue(new ByteArrayResource(content));
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, JdbcUtils.TYPE_UNKNOWN, null);
		verify(ps).setBinaryStream(eq(1), any(ByteArrayInputStream.class), eq(3L));
	}

	@Test
	void withResourceForBlob() throws SQLException {
		InputStream content = new ByteArrayInputStream(new byte[] {0, 1, 2});
		SqlBinaryValue value = new SqlBinaryValue(() -> content, 3);
		PreparedStatement ps = mock();
		value.setTypeValue(ps, 1, Types.BLOB, null);
		verify(ps).setBlob(eq(1), any(ByteArrayInputStream.class), eq(3L));
	}

}
