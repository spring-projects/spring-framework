/*
 * Copyright 2002-2014 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Default implementation of the {@link LobHandler} interface.
 * Invokes the direct accessor methods that {@code java.sql.ResultSet}
 * and {@code java.sql.PreparedStatement} offer.
 *
 * <p>By default, incoming streams are going to be passed to the appropriate
 * {@code setBinary/Ascii/CharacterStream} method on the JDBC driver's
 * {@link PreparedStatement}. If the specified content length is negative,
 * this handler will use the JDBC 4.0 variants of the set-stream methods
 * without a length parameter; otherwise, it will pass the specified length
 * on to the driver.
 *
 * <p>This LobHandler should work for any JDBC driver that is JDBC compliant
 * in terms of the spec's suggestions regarding simple BLOB and CLOB handling.
 * This does not apply to Oracle 9i's drivers at all; as of Oracle 10g,
 * it does work but may still come with LOB size limitations. Consider using
 * recent Oracle drivers even when working against an older database server.
 * See the {@link LobHandler} javadoc for the full set of recommendations.
 *
 * <p>Some JDBC drivers require values with a BLOB/CLOB target column to be
 * explicitly set through the JDBC {@code setBlob} / {@code setClob} API:
 * for example, PostgreSQL's driver. Switch the {@link #setWrapAsLob "wrapAsLob"}
 * property to "true" when operating against such a driver.
 *
 * <p>On JDBC 4.0, this LobHandler also supports streaming the BLOB/CLOB content
 * via the {@code setBlob} / {@code setClob} variants that take a stream
 * argument directly. Consider switching the {@link #setStreamAsLob "streamAsLob"}
 * property to "true" when operating against a fully compliant JDBC 4.0 driver.
 *
 * <p>Finally, primarily as a direct equivalent to {@link OracleLobHandler},
 * this LobHandler also supports the creation of temporary BLOB/CLOB objects.
 * Consider switching the {@link #setCreateTemporaryLob "createTemporaryLob"}
 * property to "true" when "streamAsLob" happens to run into LOB size limitations.
 *
 * <p>See the {@link LobHandler} interface javadoc for a summary of recommendations.
 *
 * @author Juergen Hoeller
 * @since 04.12.2003
 * @see java.sql.ResultSet#getBytes
 * @see java.sql.ResultSet#getBinaryStream
 * @see java.sql.ResultSet#getString
 * @see java.sql.ResultSet#getAsciiStream
 * @see java.sql.ResultSet#getCharacterStream
 * @see java.sql.PreparedStatement#setBytes
 * @see java.sql.PreparedStatement#setBinaryStream
 * @see java.sql.PreparedStatement#setString
 * @see java.sql.PreparedStatement#setAsciiStream
 * @see java.sql.PreparedStatement#setCharacterStream
 */
public class DefaultLobHandler extends AbstractLobHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean wrapAsLob = false;

	private boolean streamAsLob = false;

	private boolean createTemporaryLob = false;


	/**
	 * Specify whether to submit a byte array / String to the JDBC driver
	 * wrapped in a JDBC Blob / Clob object, using the JDBC {@code setBlob} /
	 * {@code setClob} method with a Blob / Clob argument.
	 * <p>Default is "false", using the common JDBC 2.0 {@code setBinaryStream}
	 * / {@code setCharacterStream} method for setting the content. Switch this
	 * to "true" for explicit Blob / Clob wrapping against JDBC drivers that
	 * are known to require such wrapping (e.g. PostgreSQL's for access to OID
	 * columns, whereas BYTEA columns need to be accessed the standard way).
	 * <p>This setting affects byte array / String arguments as well as stream
	 * arguments, unless {@link #setStreamAsLob "streamAsLob"} overrides this
	 * handling to use JDBC 4.0's new explicit streaming support (if available).
	 * @see java.sql.PreparedStatement#setBlob(int, java.sql.Blob)
	 * @see java.sql.PreparedStatement#setClob(int, java.sql.Clob)
	 */
	public void setWrapAsLob(boolean wrapAsLob) {
		this.wrapAsLob = wrapAsLob;
	}

	/**
	 * Specify whether to submit a binary stream / character stream to the JDBC
	 * driver as explicit LOB content, using the JDBC 4.0 {@code setBlob} /
	 * {@code setClob} method with a stream argument.
	 * <p>Default is "false", using the common JDBC 2.0 {@code setBinaryStream}
	 * / {@code setCharacterStream} method for setting the content.
	 * Switch this to "true" for explicit JDBC 4.0 streaming, provided that your
	 * JDBC driver actually supports those JDBC 4.0 operations (e.g. Derby's).
	 * <p>This setting affects stream arguments as well as byte array / String
	 * arguments, requiring JDBC 4.0 support. For supporting LOB content against
	 * JDBC 3.0, check out the {@link #setWrapAsLob "wrapAsLob"} setting.
	 * @see java.sql.PreparedStatement#setBlob(int, java.io.InputStream, long)
	 * @see java.sql.PreparedStatement#setClob(int, java.io.Reader, long)
	 */
	public void setStreamAsLob(boolean streamAsLob) {
		this.streamAsLob = streamAsLob;
	}

	/**
	 * Specify whether to copy a byte array / String into a temporary JDBC
	 * Blob / Clob object created through the JDBC 4.0 {@code createBlob} /
	 * {@code createClob} methods.
	 * <p>Default is "false", using the common JDBC 2.0 {@code setBinaryStream}
	 * / {@code setCharacterStream} method for setting the content. Switch this
	 * to "true" for explicit Blob / Clob creation using JDBC 4.0.
	 * <p>This setting affects stream arguments as well as byte array / String
	 * arguments, requiring JDBC 4.0 support. For supporting LOB content against
	 * JDBC 3.0, check out the {@link #setWrapAsLob "wrapAsLob"} setting.
	 * @see java.sql.Connection#createBlob()
	 * @see java.sql.Connection#createClob()
	 */
	public void setCreateTemporaryLob(boolean createTemporaryLob) {
		this.createTemporaryLob = createTemporaryLob;
	}


	@Override
	public byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning BLOB as bytes");
		if (this.wrapAsLob) {
			Blob blob = rs.getBlob(columnIndex);
			return blob.getBytes(1, (int) blob.length());
		}
		else {
			return rs.getBytes(columnIndex);
		}
	}

	@Override
	public InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning BLOB as binary stream");
		if (this.wrapAsLob) {
			Blob blob = rs.getBlob(columnIndex);
			return blob.getBinaryStream();
		}
		else {
			return rs.getBinaryStream(columnIndex);
		}
	}

	@Override
	public String getClobAsString(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as string");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getSubString(1, (int) clob.length());
		}
		else {
			return rs.getString(columnIndex);
		}
	}

	@Override
	public InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as ASCII stream");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getAsciiStream();
		}
		else {
			return rs.getAsciiStream(columnIndex);
		}
	}

	@Override
	public Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning CLOB as character stream");
		if (this.wrapAsLob) {
			Clob clob = rs.getClob(columnIndex);
			return clob.getCharacterStream();
		}
		else {
			return rs.getCharacterStream(columnIndex);
		}
	}

	@Override
	@SuppressWarnings("resource")
	public LobCreator getLobCreator() {
		return (this.createTemporaryLob ? new TemporaryLobCreator() : new DefaultLobCreator());
	}


	/**
	 * Default LobCreator implementation as an inner class.
	 * Can be subclassed in DefaultLobHandler extensions.
	 */
	protected class DefaultLobCreator implements LobCreator {

		@Override
		public void setBlobAsBytes(PreparedStatement ps, int paramIndex, byte[] content)
				throws SQLException {

			if (streamAsLob) {
				if (content != null) {
					ps.setBlob(paramIndex, new ByteArrayInputStream(content), content.length);
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (wrapAsLob) {
				if (content != null) {
					ps.setBlob(paramIndex, new PassThroughBlob(content));
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else {
				ps.setBytes(paramIndex, content);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(content != null ? "Set bytes for BLOB with length " + content.length :
						"Set BLOB to null");
			}
		}

		@Override
		public void setBlobAsBinaryStream(
				PreparedStatement ps, int paramIndex, InputStream binaryStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (binaryStream != null) {
					if (contentLength >= 0) {
						ps.setBlob(paramIndex, binaryStream, contentLength);
					}
					else {
						ps.setBlob(paramIndex, binaryStream);
					}
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (wrapAsLob) {
				if (binaryStream != null) {
					ps.setBlob(paramIndex, new PassThroughBlob(binaryStream, contentLength));
				}
				else {
					ps.setBlob(paramIndex, (Blob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setBinaryStream(paramIndex, binaryStream, contentLength);
			}
			else {
				ps.setBinaryStream(paramIndex, binaryStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(binaryStream != null ? "Set binary stream for BLOB with length " + contentLength :
						"Set BLOB to null");
			}
		}

		@Override
		public void setClobAsString(PreparedStatement ps, int paramIndex, String content)
				throws SQLException {

			if (streamAsLob) {
				if (content != null) {
					ps.setClob(paramIndex, new StringReader(content), content.length());
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (content != null) {
					ps.setClob(paramIndex, new PassThroughClob(content));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else {
				ps.setString(paramIndex, content);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(content != null ? "Set string for CLOB with length " + content.length() :
						"Set CLOB to null");
			}
		}

		@Override
		public void setClobAsAsciiStream(
				PreparedStatement ps, int paramIndex, InputStream asciiStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (asciiStream != null) {
					try {
						Reader reader = new InputStreamReader(asciiStream, "US-ASCII");
						if (contentLength >= 0) {
							ps.setClob(paramIndex, reader, contentLength);
						}
						else {
							ps.setClob(paramIndex, reader);
						}
					}
					catch (UnsupportedEncodingException ex) {
						throw new SQLException("US-ASCII encoding not supported: " + ex);
					}
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (asciiStream != null) {
					ps.setClob(paramIndex, new PassThroughClob(asciiStream, contentLength));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setAsciiStream(paramIndex, asciiStream, contentLength);
			}
			else {
				ps.setAsciiStream(paramIndex, asciiStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(asciiStream != null ? "Set ASCII stream for CLOB with length " + contentLength :
						"Set CLOB to null");
			}
		}

		@Override
		public void setClobAsCharacterStream(
				PreparedStatement ps, int paramIndex, Reader characterStream, int contentLength)
				throws SQLException {

			if (streamAsLob) {
				if (characterStream != null) {
					if (contentLength >= 0) {
						ps.setClob(paramIndex, characterStream, contentLength);
					}
					else {
						ps.setClob(paramIndex, characterStream);
					}
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (wrapAsLob) {
				if (characterStream != null) {
					ps.setClob(paramIndex, new PassThroughClob(characterStream, contentLength));
				}
				else {
					ps.setClob(paramIndex, (Clob) null);
				}
			}
			else if (contentLength >= 0) {
				ps.setCharacterStream(paramIndex, characterStream, contentLength);
			}
			else {
				ps.setCharacterStream(paramIndex, characterStream);
			}
			if (logger.isDebugEnabled()) {
				logger.debug(characterStream != null ? "Set character stream for CLOB with length " + contentLength :
						"Set CLOB to null");
			}
		}

		@Override
		public void close() {
			// nothing to do when not creating temporary LOBs
		}
	}

}
