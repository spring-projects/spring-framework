/*
 * Copyright 2002-2013 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.FileCopyUtils;

/**
 * {@link LobCreator} implementation based on temporary LOBs,
 * using JDBC 4.0's {@link java.sql.Connection#createBlob()} /
 * {@link java.sql.Connection#createClob()} mechanism.
 *
 * <p>Used by DefaultLobHandler's {@link DefaultLobHandler#setCreateTemporaryLob} mode.
 * Can also be used directly to reuse the tracking and freeing of temporary LOBs.
 *
 * @author Juergen Hoeller
 * @since 3.2.2
 * @see DefaultLobHandler#setCreateTemporaryLob
 * @see java.sql.Connection#createBlob()
 * @see java.sql.Connection#createClob()
 */
public class TemporaryLobCreator implements LobCreator {

	protected static final Log logger = LogFactory.getLog(TemporaryLobCreator.class);

	private final Set<Blob> temporaryBlobs = new LinkedHashSet<Blob>(1);

	private final Set<Clob> temporaryClobs = new LinkedHashSet<Clob>(1);


	public void setBlobAsBytes(PreparedStatement ps, int paramIndex, byte[] content)
			throws SQLException {

		Blob blob = ps.getConnection().createBlob();
		blob.setBytes(1, content);

		this.temporaryBlobs.add(blob);
		ps.setBlob(paramIndex, blob);

		if (logger.isDebugEnabled()) {
			logger.debug(content != null ? "Copied bytes into temporary BLOB with length " + content.length :
					"Set BLOB to null");
		}
	}

	public void setBlobAsBinaryStream(
			PreparedStatement ps, int paramIndex, InputStream binaryStream, int contentLength)
			throws SQLException {

		Blob blob = ps.getConnection().createBlob();
		try {
			FileCopyUtils.copy(binaryStream, blob.setBinaryStream(1));
		}
		catch (IOException ex) {
			throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
		}

		this.temporaryBlobs.add(blob);
		ps.setBlob(paramIndex, blob);

		if (logger.isDebugEnabled()) {
			logger.debug(binaryStream != null ?
					"Copied binary stream into temporary BLOB with length " + contentLength :
					"Set BLOB to null");
		}
	}

	public void setClobAsString(PreparedStatement ps, int paramIndex, String content)
			throws SQLException {

		Clob clob = ps.getConnection().createClob();
		clob.setString(1, content);

		this.temporaryClobs.add(clob);
		ps.setClob(paramIndex, clob);

		if (logger.isDebugEnabled()) {
			logger.debug(content != null ? "Copied string into temporary CLOB with length " + content.length() :
					"Set CLOB to null");
		}
	}

	public void setClobAsAsciiStream(
			PreparedStatement ps, int paramIndex, InputStream asciiStream, int contentLength)
			throws SQLException {

		Clob clob = ps.getConnection().createClob();
		try {
			FileCopyUtils.copy(asciiStream, clob.setAsciiStream(1));
		}
		catch (IOException ex) {
			throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
		}

		this.temporaryClobs.add(clob);
		ps.setClob(paramIndex, clob);

		if (logger.isDebugEnabled()) {
			logger.debug(asciiStream != null ?
					"Copied ASCII stream into temporary CLOB with length " + contentLength :
					"Set CLOB to null");
		}
	}

	public void setClobAsCharacterStream(
			PreparedStatement ps, int paramIndex, Reader characterStream, int contentLength)
			throws SQLException {

		Clob clob = ps.getConnection().createClob();
		try {
			FileCopyUtils.copy(characterStream, clob.setCharacterStream(1));
		}
		catch (IOException ex) {
			throw new DataAccessResourceFailureException("Could not copy into LOB stream", ex);
		}

		this.temporaryClobs.add(clob);
		ps.setClob(paramIndex, clob);

		if (logger.isDebugEnabled()) {
			logger.debug(characterStream != null ?
					"Copied character stream into temporary CLOB with length " + contentLength :
					"Set CLOB to null");
		}
	}

	public void close() {
		try {
			for (Blob blob : this.temporaryBlobs) {
				blob.free();
			}
			for (Clob clob : this.temporaryClobs) {
				clob.free();
			}
		}
		catch (SQLException ex) {
			logger.error("Could not free LOB", ex);
		}
	}
}
