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

package org.springframework.jdbc.core.support;

import java.io.InputStream;
import java.io.Reader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.DisposableSqlTypeValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;

/**
 * Object to represent an SQL BLOB/CLOB value parameter. BLOBs can either be an
 * InputStream or a byte array. CLOBs can be in the form of a Reader, InputStream,
 * or String. Each CLOB/BLOB value will be stored together with its length.
 * The type is based on which constructor is used. Instances of this class are
 * stateful and immutable: use them and discard them.
 *
 * <p><b>NOTE: As of 6.1.4, this class is effectively superseded by
 * {@link SqlBinaryValue} and {@link SqlCharacterValue} which are capable of
 * modern BLOB/CLOB handling while also handling LONGVARBINARY/LONGVARCHAR.</b>
 * The only reason to keep using this class is a custom {@link LobHandler}.
 *
 * <p>This class holds a reference to a {@link LobCreator} that must be closed after
 * the update has completed. This is done via a call to the {@link #cleanup()} method.
 * All handling of the {@code LobCreator} is done by the framework classes that use it -
 * no need to set or close the {@code LobCreator} for end users of this class.
 *
 * <p>A usage example:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.update(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new Object[] {
 *       name,
 *       new SqlLobValue(contentStream, contentLength, lobHandler),
 *       new SqlLobValue(description, lobHandler)
 *     },
 *     new int[] {Types.VARCHAR, Types.BLOB, Types.CLOB});
 * </pre>
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jdbc.support.lob.LobHandler
 * @see org.springframework.jdbc.support.lob.LobCreator
 * @see org.springframework.jdbc.core.JdbcTemplate#update(String, Object[], int[])
 * @see org.springframework.jdbc.object.SqlUpdate#update(Object[])
 * @see org.springframework.jdbc.object.StoredProcedure#execute(java.util.Map)
 * @deprecated as of 6.2, in favor of {@link SqlBinaryValue} and {@link SqlCharacterValue}
 */
@Deprecated(since = "6.2")
public class SqlLobValue implements DisposableSqlTypeValue {

	private final @Nullable Object content;

	private final int length;

	/**
	 * Reference to the LobCreator - so we can close it once the update is done.
	 */
	private final LobCreator lobCreator;


	/**
	 * Create a new BLOB value with the given byte array,
	 * using a DefaultLobHandler.
	 * @param bytes the byte array containing the BLOB value
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(byte @Nullable [] bytes) {
		this(bytes, new DefaultLobHandler());
	}

	/**
	 * Create a new BLOB value with the given byte array.
	 * @param bytes the byte array containing the BLOB value
	 * @param lobHandler the LobHandler to be used
	 */
	public SqlLobValue(byte @Nullable [] bytes, LobHandler lobHandler) {
		this.content = bytes;
		this.length = (bytes != null ? bytes.length : 0);
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * Create a new CLOB value with the given content string,
	 * using a DefaultLobHandler.
	 * @param content the String containing the CLOB value
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(@Nullable String content) {
		this(content, new DefaultLobHandler());
	}

	/**
	 * Create a new CLOB value with the given content string.
	 * @param content the String containing the CLOB value
	 * @param lobHandler the LobHandler to be used
	 */
	public SqlLobValue(@Nullable String content, LobHandler lobHandler) {
		this.content = content;
		this.length = (content != null ? content.length() : 0);
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * Create a new BLOB/CLOB value with the given stream,
	 * using a DefaultLobHandler.
	 * @param stream the stream containing the LOB value
	 * @param length the length of the LOB value
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(InputStream stream, int length) {
		this(stream, length, new DefaultLobHandler());
	}

	/**
	 * Create a new BLOB/CLOB value with the given stream.
	 * @param stream the stream containing the LOB value
	 * @param length the length of the LOB value
	 * @param lobHandler the LobHandler to be used
	 */
	public SqlLobValue(InputStream stream, int length, LobHandler lobHandler) {
		this.content = stream;
		this.length = length;
		this.lobCreator = lobHandler.getLobCreator();
	}

	/**
	 * Create a new CLOB value with the given character stream,
	 * using a DefaultLobHandler.
	 * @param reader the character stream containing the CLOB value
	 * @param length the length of the CLOB value
	 * @see org.springframework.jdbc.support.lob.DefaultLobHandler
	 */
	public SqlLobValue(Reader reader, int length) {
		this(reader, length, new DefaultLobHandler());
	}

	/**
	 * Create a new CLOB value with the given character stream.
	 * @param reader the character stream containing the CLOB value
	 * @param length the length of the CLOB value
	 * @param lobHandler the LobHandler to be used
	 */
	public SqlLobValue(Reader reader, int length, LobHandler lobHandler) {
		this.content = reader;
		this.length = length;
		this.lobCreator = lobHandler.getLobCreator();
	}


	/**
	 * Set the specified content via the LobCreator.
	 */
	@Override
	public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		if (sqlType == Types.BLOB) {
			if (this.content instanceof byte[] || this.content == null) {
				this.lobCreator.setBlobAsBytes(ps, paramIndex, (byte[]) this.content);
			}
			else if (this.content instanceof String string) {
				this.lobCreator.setBlobAsBytes(ps, paramIndex, string.getBytes());
			}
			else if (this.content instanceof InputStream inputStream) {
				this.lobCreator.setBlobAsBinaryStream(ps, paramIndex, inputStream, this.length);
			}
			else {
				throw new IllegalArgumentException(
						"Content type [" + this.content.getClass().getName() + "] not supported for BLOB columns");
			}
		}
		else if (sqlType == Types.CLOB) {
			if (this.content instanceof String || this.content == null) {
				this.lobCreator.setClobAsString(ps, paramIndex, (String) this.content);
			}
			else if (this.content instanceof InputStream inputStream) {
				this.lobCreator.setClobAsAsciiStream(ps, paramIndex, inputStream, this.length);
			}
			else if (this.content instanceof Reader reader) {
				this.lobCreator.setClobAsCharacterStream(ps, paramIndex, reader, this.length);
			}
			else {
				throw new IllegalArgumentException(
						"Content type [" + this.content.getClass().getName() + "] not supported for CLOB columns");
			}
		}
		else {
			throw new IllegalArgumentException("SqlLobValue only supports SQL types BLOB and CLOB");
		}
	}

	/**
	 * Close the LobCreator.
	 */
	@Override
	public void cleanup() {
		this.lobCreator.close();
	}

}
