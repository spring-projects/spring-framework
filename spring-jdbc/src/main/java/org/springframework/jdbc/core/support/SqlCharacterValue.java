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

import java.io.CharArrayReader;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.lang.Nullable;

/**
 * Object to represent a character-based parameter value for a SQL statement,
 * e.g. a character stream for a CLOB/NCLOB or a LONGVARCHAR column.
 *
 * <p>Designed for use with {@link org.springframework.jdbc.core.JdbcTemplate}
 * as well as {@link org.springframework.jdbc.core.simple.JdbcClient}, to be
 * passed in as a parameter value wrapping the target content value. Can be
 * combined with {@link org.springframework.jdbc.core.SqlParameterValue} for
 * specifying a SQL type, e.g.
 * {@code new SqlParameterValue(Types.CLOB, new SqlCharacterValue(myContent))}.
 * With most database drivers, the type hint is not actually necessary.
 *
 * @author Juergen Hoeller
 * @since 6.1.4
 * @see SqlBinaryValue
 * @see org.springframework.jdbc.core.SqlParameterValue
 */
public class SqlCharacterValue implements SqlTypeValue {

	private final Object content;

	private final long length;


	/**
	 * Create a new CLOB value with the given content string.
	 * @param string the content as a String or other CharSequence
	 */
	public SqlCharacterValue(CharSequence string) {
		this.content = string;
		this.length = string.length();
	}

	/**
	 * Create a new {@code SqlCharacterValue} for the given content.
	 * @param characters the content as a character array
	 */
	public SqlCharacterValue(char[] characters) {
		this.content = characters;
		this.length = characters.length;
	}

	/**
	 * Create a new {@code SqlCharacterValue} for the given content.
	 * @param reader the content reader
	 * @param length the length of the content
	 */
	public SqlCharacterValue(Reader reader, long length) {
		this.content = reader;
		this.length = length;
	}

	/**
	 * Create a new {@code SqlCharacterValue} for the given content.
	 * @param asciiStream the content as ASCII stream
	 * @param length the length of the content
	 */
	public SqlCharacterValue(InputStream asciiStream, long length) {
		this.content = asciiStream;
		this.length = length;
	}


	@Override
	public void setTypeValue(PreparedStatement ps, int paramIndex, int sqlType, @Nullable String typeName)
			throws SQLException {

		if (this.content instanceof CharSequence) {
			setString(ps, paramIndex, sqlType, this.content.toString());
		}
		else if (this.content instanceof char[] chars) {
			setReader(ps, paramIndex, sqlType, new CharArrayReader(chars), this.length);
		}
		else if (this.content instanceof Reader reader) {
			setReader(ps, paramIndex, sqlType, reader, this.length);
		}
		else if (this.content instanceof InputStream asciiStream) {
			ps.setAsciiStream(paramIndex, asciiStream, this.length);
		}
		else {
			throw new IllegalArgumentException("Illegal content type: " + this.content.getClass().getName());
		}
	}

	private void setString(PreparedStatement ps, int paramIndex, int sqlType, String string)
			throws SQLException {

		if (sqlType == Types.CLOB) {
			ps.setClob(paramIndex, new StringReader(string), string.length());
		}
		else if (sqlType == Types.NCLOB) {
			ps.setNClob(paramIndex, new StringReader(string), string.length());
		}
		else {
			ps.setString(paramIndex, string);
		}
	}

	private void setReader(PreparedStatement ps, int paramIndex, int sqlType, Reader reader, long length)
			throws SQLException {

		if (sqlType == Types.CLOB) {
			ps.setClob(paramIndex, reader, length);
		}
		else if (sqlType == Types.NCLOB) {
			ps.setNClob(paramIndex, reader, length);
		}
		else {
			ps.setCharacterStream(paramIndex, reader, length);
		}
	}

}
