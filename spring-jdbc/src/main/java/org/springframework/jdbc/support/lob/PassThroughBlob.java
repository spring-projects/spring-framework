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
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.springframework.lang.Nullable;

/**
 * Simple JDBC {@link Blob} adapter that exposes a given byte array or binary stream.
 * Optionally used by {@link DefaultLobHandler}.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 */
class PassThroughBlob implements Blob {

	@Nullable
	private byte[] content;

	@Nullable
	private InputStream binaryStream;

	private final long contentLength;


	public PassThroughBlob(byte[] content) {
		this.content = content;
		this.contentLength = content.length;
	}

	public PassThroughBlob(InputStream binaryStream, long contentLength) {
		this.binaryStream = binaryStream;
		this.contentLength = contentLength;
	}


	@Override
	public long length() throws SQLException {
		return this.contentLength;
	}

	@Override
	public InputStream getBinaryStream() throws SQLException {
		if (this.content != null) {
			return new ByteArrayInputStream(this.content);
		}
		else {
			return (this.binaryStream != null ? this.binaryStream : InputStream.nullInputStream());
		}
	}


	@Override
	public InputStream getBinaryStream(long pos, long length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public OutputStream setBinaryStream(long pos) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public byte[] getBytes(long pos, int length) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int setBytes(long pos, byte[] bytes) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position(byte[] pattern, long start) throws SQLException {
		throw new UnsupportedOperationException();
	}

	@Override
	public long position(Blob pattern, long start) throws SQLException {
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
