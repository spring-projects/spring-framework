/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.jdbc.support.lob;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;

import org.springframework.lang.Nullable;
import org.springframework.util.FileCopyUtils;

/**
 * Simple JDBC {@link Clob} adapter that exposes a given String or character stream.
 * Optionally used by {@link DefaultLobHandler}.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 */
class PassThroughClob implements Clob {

	@Nullable
	private String content;

	@Nullable
	private Reader characterStream;

	@Nullable
	private InputStream asciiStream;

	private final long contentLength;


	public PassThroughClob(String content) {
		this.content = content;
		this.contentLength = content.length();
	}

	public PassThroughClob(Reader characterStream, long contentLength) {
		this.characterStream = characterStream;
		this.contentLength = contentLength;
	}

	public PassThroughClob(InputStream asciiStream, long contentLength) {
		this.asciiStream = asciiStream;
		this.contentLength = contentLength;
	}


	@Override
	public long length() throws SQLException {
		return this.contentLength;
	}

	@Override
	public Reader getCharacterStream() throws SQLException {
		if (this.content != null) {
			return new StringReader(this.content);
		}
		else if (this.characterStream != null) {
			return this.characterStream;
		}
		else {
			return new InputStreamReader(
					(this.asciiStream != null ? this.asciiStream : InputStream.nullInputStream()),
					StandardCharsets.US_ASCII);
		}
	}

	@Override
	public InputStream getAsciiStream() throws SQLException {
		try {
			if (this.content != null) {
				return new ByteArrayInputStream(this.content.getBytes(StandardCharsets.US_ASCII));
			}
			else if (this.characterStream != null) {
				String tempContent = FileCopyUtils.copyToString(this.characterStream);
				return new ByteArrayInputStream(tempContent.getBytes(StandardCharsets.US_ASCII));
			}
			else {
				return (this.asciiStream != null ? this.asciiStream : InputStream.nullInputStream());
			}
		}
		catch (IOException ex) {
			throw new SQLException("Failed to read stream content: " + ex);
		}
	}


	@Override
	public Reader getCharacterStream(long pos, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public Writer setCharacterStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream setAsciiStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getSubString(long pos, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int setString(long pos, String str) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int setString(long pos, String str, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position(String searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position(Clob searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void truncate(long len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void free() throws SQLException {
		// no-op
	}

}
