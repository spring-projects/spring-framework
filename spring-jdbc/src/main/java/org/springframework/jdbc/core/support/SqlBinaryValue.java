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
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.lang.Nullable;

/**
 * Object to represent a binary parameter value for a SQL statement, e.g.
 * a binary stream for a BLOB or a LONGVARBINARY or PostgreSQL BYTEA column.
 *
 * <p>Designed for use with {@link org.springframework.jdbc.core.JdbcTemplate}
 * as well as {@link org.springframework.jdbc.core.simple.JdbcClient}, to be
 * passed in as a parameter value wrapping the target content value. Can be
 * combined with {@link org.springframework.jdbc.core.SqlParameterValue} for
 * specifying a SQL type, e.g.
 * {@code new SqlParameterValue(Types.BLOB, new SqlBinaryValue(myContent))}.
 * With most database drivers, the type hint is not actually necessary.
 *
 * @author Juergen Hoeller
 * @since 6.1.4
 * @see SqlCharacterValue
 * @see org.springframework.jdbc.core.SqlParameterValue
 */
public class SqlBinaryValue implements SqlTypeValue {

	private final Object content;

	private final long length;


	/**
	 * Create a new {@code SqlBinaryValue} for the given content.
	 * @param bytes the content as a byte array
	 */
	public SqlBinaryValue(byte[] bytes) {
		this.content = bytes;
		this.length = bytes.length;
	}

	/**
	 * Create a new {@code SqlBinaryValue} for the given content.
	 * @param stream the content stream
	 * @param length the length of the content
	 */
	public SqlBinaryValue(InputStream stream, long length) {
		this.content = stream;
		this.length = length;
	}

	/**
	 * Create a new {@code SqlBinaryValue} for the given content.
	 * <p>Consider specifying a {@link Resource} with content length support
	 * when available: {@link SqlBinaryValue#SqlBinaryValue(Resource)}.
	 * @param resource the resource to obtain a content stream from
	 * @param length the length of the content
	 */
	public SqlBinaryValue(InputStreamSource resource, long length) {
		this.content = resource;
		this.length = length;
	}

	/**
	 * Create a new {@code SqlBinaryValue} for the given content.
	 * <p>The length will get derived from {@link Resource#contentLength()}.
	 * @param resource the resource to obtain a content stream from
	 */
	public SqlBinaryValue(Resource resource) {
		this.content = resource;
		this.length = -1;
	}


	@Override
	public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		if (this.content instanceof byte[] bytes) {
			setByteArray(ps, paramIndex, sqlType, bytes);
		}
		else if (this.content instanceof InputStream inputStream) {
			setInputStream(ps, paramIndex, sqlType, inputStream, this.length);
		}
		else if (this.content instanceof Resource resource) {
			try {
				setInputStream(ps, paramIndex, sqlType, resource.getInputStream(), resource.contentLength());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot open binary stream for JDBC value: " + resource, ex);
			}
		}
		else if (this.content instanceof InputStreamSource resource) {
			try {
				setInputStream(ps, paramIndex, sqlType, resource.getInputStream(), this.length);
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot open binary stream for JDBC value: " + resource, ex);
			}
		}
		else {
			throw new IllegalArgumentException("Illegal content type: " + this.content.getClass().getName());
		}
	}

	private void setByteArray(PreparedStatement ps, int paramIndex, int sqlType, byte[] bytes)
			throws SQLException {

		if (sqlType == Types.BLOB) {
			ps.setBlob(paramIndex, new ByteArrayInputStream(bytes), bytes.length);
		}
		else {
			ps.setBytes(paramIndex, bytes);
		}
	}

	private void setInputStream(PreparedStatement ps, int paramIndex, int sqlType, InputStream is, long length)
			throws SQLException {

		if (sqlType == Types.BLOB) {
			ps.setBlob(paramIndex, is, length);
		}
		else {
			ps.setBinaryStream(paramIndex, is, length);
		}
	}

}
