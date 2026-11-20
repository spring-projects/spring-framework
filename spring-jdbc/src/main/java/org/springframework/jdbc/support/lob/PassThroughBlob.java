/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support.lob;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * Simple JDBC {@link Blob} adapter that exposes a given byte array or binary stream.
 * Optionally used by {@link DefaultLobHandler}.
 *
 * @author Juergen Hoeller
 * @since 2.5.3
 */
@Deprecated(since = "6.2")
class PassThroughBlob implements Blob {

	private byte @Nullable [] content;

	private @Nullable InputStream binaryStream;

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
