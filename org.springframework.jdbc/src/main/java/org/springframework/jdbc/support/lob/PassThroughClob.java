/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.support.lob;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Clob;
import java.sql.SQLException;

/**
 * Simple JDBC {@link Clob} adapter that exposes a given String or character stream.
 * Optionally used by {@link DefaultLobHandler}.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 */
class PassThroughClob implements Clob {

	private String content;

	private Reader characterStream;

	private long contentLength;


	public PassThroughClob(String content) {
		this.content = content;
		this.contentLength = content.length();
	}

	public PassThroughClob(Reader characterStream, long contentLength) {
		this.characterStream = characterStream;
		this.contentLength = contentLength;
	}


	public long length() throws SQLException {
		return this.contentLength;
	}

	public Reader getCharacterStream() throws SQLException {
		return (this.content != null ? new StringReader(this.content) : this.characterStream);
	}


	public Reader getCharacterStream(long pos, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public Writer setCharacterStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public InputStream getAsciiStream() throws SQLException {
		return null;
	}

	public OutputStream setAsciiStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public String getSubString(long pos, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int setString(long pos, String str) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public int setString(long pos, String str, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public long position(String searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public long position(Clob searchstr, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void truncate(long len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	public void free() throws SQLException {
		throw new UnsupportedOperationException();
	}

}
