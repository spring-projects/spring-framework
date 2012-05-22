/*
 * Copyright 2002-2012 the original author or authors.
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
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.util.FileCopyUtils;

/**
 * {@link LobHandler} implementation for Oracle databases. Uses proprietary API
 * to create <code>oracle.sql.BLOB</code> and <code>oracle.sql.CLOB</code>
 * instances, as necessary when working with Oracle's JDBC driver.
 * Note that this LobHandler requires Oracle JDBC driver 9i or higher!
 *
 * <p>While most databases are able to work with {@link DefaultLobHandler},
 * Oracle just accepts Blob/Clob instances created via its own proprietary
 * BLOB/CLOB API, and additionally doesn't accept large streams for
 * PreparedStatement's corresponding setter methods. Therefore, you need
 * to use a strategy like this LobHandler implementation.
 *
 * <p>Needs to work on a native JDBC Connection, to be able to cast it to
 * <code>oracle.jdbc.OracleConnection</code>. If you pass in Connections from a
 * connection pool (the usual case in a J2EE environment), you need to set an
 * appropriate {@link org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor}
 * to allow for automatic retrieval of the underlying native JDBC Connection.
 * LobHandler and NativeJdbcExtractor are separate concerns, therefore they
 * are represented by separate strategy interfaces.
 *
 * <p>Coded via reflection to avoid dependencies on Oracle classes.
 * Even reads in Oracle constants via reflection because of different Oracle
 * drivers (classes12, ojdbc14, ojdbc5, ojdbc6) having different constant values!
 * As this LobHandler initializes Oracle classes on instantiation, do not define
 * this as eager-initializing singleton if you do not want to depend on the Oracle
 * JAR being in the class path: use "lazy-init=true" to avoid this issue.
 *
 * @author Juergen Hoeller
 * @author Thomas Risberg
 * @since 04.12.2003
 * @see #setNativeJdbcExtractor
 */
public class OracleLobHandler extends AbstractLobHandler {

	private static final String BLOB_CLASS_NAME = "oracle.sql.BLOB";

	private static final String CLOB_CLASS_NAME = "oracle.sql.CLOB";

	private static final String DURATION_SESSION_FIELD_NAME = "DURATION_SESSION";

	private static final String MODE_READWRITE_FIELD_NAME = "MODE_READWRITE";

	private static final String MODE_READONLY_FIELD_NAME = "MODE_READONLY";


	protected final Log logger = LogFactory.getLog(getClass());

	private NativeJdbcExtractor nativeJdbcExtractor;

	private Boolean cache = Boolean.TRUE;

	private Boolean releaseResourcesAfterRead = Boolean.FALSE;

	private Class<?> blobClass;

	private Class<?> clobClass;

	private final Map<Class<?>, Integer> durationSessionConstants = new HashMap<Class<?>, Integer>(2);

	private final Map<Class<?>, Integer> modeReadWriteConstants = new HashMap<Class<?>, Integer>(2);

	private final Map<Class<?>, Integer> modeReadOnlyConstants = new HashMap<Class<?>, Integer>(2);


	/**
	 * Set an appropriate NativeJdbcExtractor to be able to retrieve the underlying
	 * native <code>oracle.jdbc.OracleConnection</code>. This is necessary for
	 * DataSource-based connection pools, as those need to return wrapped JDBC
	 * Connection handles that cannot be cast to a native Connection implementation.
	 * <p>Effectively, this LobHandler just invokes a single NativeJdbcExtractor
	 * method, namely <code>getNativeConnectionFromStatement</code> with a
	 * PreparedStatement argument (falling back to a
	 * <code>PreparedStatement.getConnection()</code> call if no extractor is set).
	 * <p>A common choice is {@code SimpleNativeJdbcExtractor}, whose Connection unwrapping
	 * (which is what OracleLobHandler needs) will work with many connection pools.
	 * See {@code SimpleNativeJdbcExtractor} and
	 * <a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/jdbc/OracleConnection.html">
	 * oracle.jdbc.OracleConnection</a> javadoc for details.
	 * @see org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor#getNativeConnectionFromStatement
	 * @see org.springframework.jdbc.support.nativejdbc.SimpleNativeJdbcExtractor
	 * @see org.springframework.jdbc.support.nativejdbc.OracleJdbc4NativeJdbcExtractor
	 */
	public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
		this.nativeJdbcExtractor = nativeJdbcExtractor;
	}

	/**
	 * Set whether to cache the temporary LOB in the buffer cache.
	 * This value will be passed into BLOB/CLOB.createTemporary.
	 * <p>Default is <code>true</code>.
	 * <p><strong>See Also:</strong>
	 * <ul>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#createTemporary()">oracle.sql.BLOB.createTemporary</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#createTemporary()">oracle.sql.CLOB.createTemporary</a></li>
	 * </ul>
	 */
	public void setCache(boolean cache) {
		this.cache = cache;
	}

	/**
	 * Set whether to agressively release any resources used by the LOB. If set to <code>true</code>
	 * then you can only read the LOB values once. Any subsequent reads will fail since the resources
	 * have been closed.
	 * <p>Setting this property to <code>true</code> can be useful when your queries generates large
	 * temporary LOBs that occupy space in the TEMPORARY tablespace or when you want to free up any
	 * memory allocated by the driver for the LOB reading.
	 * <p>Default is <code>false</code>.
	 * <p><strong>See Also:</strong>
	 * <ul>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#freeTemporary()">oracle.sql.BLOB.freeTemporary</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#freeTemporary()">oracle.sql.CLOB.freeTemporary</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#open()">oracle.sql.BLOB.open</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#open()">oracle.sql.CLOB.open</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#open()">oracle.sql.BLOB.close</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#open()">oracle.sql.CLOB.close</a></li>
	 * </ul>
	 */
	public void setReleaseResourcesAfterRead(boolean releaseResources) {
		this.releaseResourcesAfterRead = releaseResources;
	}


	/**
	 * Retrieve the <code>oracle.sql.BLOB</code> and <code>oracle.sql.CLOB</code>
	 * classes via reflection, and initialize the values for the
	 * DURATION_SESSION, MODE_READWRITE and MODE_READONLY constants defined there.
	 * <p><strong>See Also:</strong>
	 * <ul>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#DURATION_SESSION">oracle.sql.BLOB.DURATION_SESSION</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#MODE_READWRITE">oracle.sql.BLOB.MODE_READWRITE</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/BLOB.html#MODE_READONLY">oracle.sql.BLOB.MODE_READONLY</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#DURATION_SESSION">oracle.sql.CLOB.DURATION_SESSION</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#MODE_READWRITE">oracle.sql.CLOB.MODE_READWRITE</a></li>
	 * <li><a href="http://download.oracle.com/otn_hosted_doc/jdeveloper/905/jdbc-javadoc/oracle/sql/CLOB.html#MODE_READONLY">oracle.sql.CLOB.MODE_READONLY</a></li>
	 * </ul>
	 * @param con the Oracle Connection, for using the exact same class loader
	 * that the Oracle driver was loaded with
	 */
	protected synchronized void initOracleDriverClasses(Connection con) {
		if (this.blobClass == null) {
			try {
				// Initialize oracle.sql.BLOB class
				this.blobClass = con.getClass().getClassLoader().loadClass(BLOB_CLASS_NAME);
				this.durationSessionConstants.put(
						this.blobClass, this.blobClass.getField(DURATION_SESSION_FIELD_NAME).getInt(null));
				this.modeReadWriteConstants.put(
						this.blobClass, this.blobClass.getField(MODE_READWRITE_FIELD_NAME).getInt(null));
				this.modeReadOnlyConstants.put(
						this.blobClass, this.blobClass.getField(MODE_READONLY_FIELD_NAME).getInt(null));

				// Initialize oracle.sql.CLOB class
				this.clobClass = con.getClass().getClassLoader().loadClass(CLOB_CLASS_NAME);
				this.durationSessionConstants.put(
						this.clobClass, this.clobClass.getField(DURATION_SESSION_FIELD_NAME).getInt(null));
				this.modeReadWriteConstants.put(
						this.clobClass, this.clobClass.getField(MODE_READWRITE_FIELD_NAME).getInt(null));
				this.modeReadOnlyConstants.put(
						this.clobClass, this.clobClass.getField(MODE_READONLY_FIELD_NAME).getInt(null));
			}
			catch (Exception ex) {
				throw new InvalidDataAccessApiUsageException(
						"Couldn't initialize OracleLobHandler because Oracle driver classes are not available. " +
						"Note that OracleLobHandler requires Oracle JDBC driver 9i or higher!", ex);
			}
		}
	}


	public byte[] getBlobAsBytes(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle BLOB as bytes");
		Blob blob = rs.getBlob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), blob);
		byte[] retVal = (blob != null ? blob.getBytes(1, (int) blob.length()) : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), blob);
		return retVal;
	}

	public InputStream getBlobAsBinaryStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle BLOB as binary stream");
		Blob blob = rs.getBlob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), blob);
		InputStream retVal = (blob != null ? blob.getBinaryStream() : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), blob);
		return retVal;
	}

	public String getClobAsString(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle CLOB as string");
		Clob clob = rs.getClob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), clob);
		String retVal = (clob != null ? clob.getSubString(1, (int) clob.length()) : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), clob);
		return retVal;
	}

	public InputStream getClobAsAsciiStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle CLOB as ASCII stream");
		Clob clob = rs.getClob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), clob);
		InputStream retVal = (clob != null ? clob.getAsciiStream() : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), clob);
		return retVal;
	}

	public Reader getClobAsCharacterStream(ResultSet rs, int columnIndex) throws SQLException {
		logger.debug("Returning Oracle CLOB as character stream");
		Clob clob = rs.getClob(columnIndex);
		initializeResourcesBeforeRead(rs.getStatement().getConnection(), clob);
		Reader retVal = (clob != null ? clob.getCharacterStream() : null);
		releaseResourcesAfterRead(rs.getStatement().getConnection(), clob);
		return retVal;
	}

	public LobCreator getLobCreator() {
		return new OracleLobCreator();
	}

	/**
	 * Initialize any LOB resources before a read is done.
	 * <p>This implementation calls <code>BLOB.open(BLOB.MODE_READONLY)</code> or
	 * <code>CLOB.open(CLOB.MODE_READONLY)</code> on any non-temporary LOBs if
	 * <code>releaseResourcesAfterRead</code> property is set to <code>true</code>.
	 * <p>This method can be overridden by sublcasses if different behavior is desired.
	 * @param con the connection to be usde for initilization
	 * @param lob the LOB to initialize
	 */
	protected void initializeResourcesBeforeRead(Connection con, Object lob) {
		if (this.releaseResourcesAfterRead) {
			initOracleDriverClasses(con);
			try {
				/*
				if (!((BLOB) lob.isTemporary() {
				*/
				Method isTemporary = lob.getClass().getMethod("isTemporary");
				Boolean temporary = (Boolean) isTemporary.invoke(lob);
				if (!temporary) {
					/*
					((BLOB) lob).open(BLOB.MODE_READONLY);
					*/
					Method open = lob.getClass().getMethod("open", int.class);
					open.invoke(lob, modeReadOnlyConstants.get(lob.getClass()));
				}
			}
			catch (InvocationTargetException ex) {
				logger.error("Could not open Oracle LOB", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new DataAccessResourceFailureException("Could not open Oracle LOB", ex);
			}
		}
	}

	/**
	 * Release any LOB resources after read is complete.
	 * <p>If <code>releaseResourcesAfterRead</code> property is set to <code>true</code>
	 * then this implementation calls
	 * <code>BLOB.close()</code> or <code>CLOB.close()</code>
	 * on any non-temporary LOBs that are open or
	 * <code>BLOB.freeTemporary()</code> or <code>CLOB.freeTemporary()</code>
	 * on any temporary LOBs.
	 * <p>This method can be overridden by sublcasses if different behavior is desired.
	 * @param con the connection to be usde for initilization
	 * @param lob the LOB to initialize
	 */
	protected void releaseResourcesAfterRead(Connection con, Object lob) {
		if (this.releaseResourcesAfterRead) {
			initOracleDriverClasses(con);
			Boolean temporary = Boolean.FALSE;
			try {
				/*
				if (((BLOB) lob.isTemporary() {
				*/
				Method isTemporary = lob.getClass().getMethod("isTemporary");
				temporary = (Boolean) isTemporary.invoke(lob);
				if (temporary) {
					/*
					((BLOB) lob).freeTemporary();
					*/
					Method freeTemporary = lob.getClass().getMethod("freeTemporary");
					freeTemporary.invoke(lob);
				}
				else {
					/*
					if (((BLOB) lob.isOpen() {
					*/
					Method isOpen = lob.getClass().getMethod("isOpen");
					Boolean open = (Boolean) isOpen.invoke(lob);
					if (open) {
						/*
						((BLOB) lob).close();
						*/
						Method close = lob.getClass().getMethod("close");
						close.invoke(lob);
					}
				}
			}
			catch (InvocationTargetException ex) {
				if (temporary) {
					logger.error("Could not free Oracle LOB", ex.getTargetException());
				}
				else {
					logger.error("Could not close Oracle LOB", ex.getTargetException());
				}
			}
			catch (Exception ex) {
				if (temporary) {
					throw new DataAccessResourceFailureException("Could not free Oracle LOB", ex);
				}
				else {
					throw new DataAccessResourceFailureException("Could not close Oracle LOB", ex);
				}
			}
		}
	}


	/**
	 * LobCreator implementation for Oracle databases.
	 * Creates Oracle-style temporary BLOBs and CLOBs that it frees on close.
	 * @see #close
	 */
	protected class OracleLobCreator implements LobCreator {

		private final List<Object> createdLobs = new LinkedList<Object>();

		public void setBlobAsBytes(PreparedStatement ps, int paramIndex, final byte[] content)
				throws SQLException {

			if (content != null) {
				Blob blob = (Blob) createLob(ps, false, new LobCallback() {
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getBinaryOutputStream");
						OutputStream out = (OutputStream) methodToInvoke.invoke(lob);
						FileCopyUtils.copy(content, out);
					}
				});
				ps.setBlob(paramIndex, blob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set bytes for Oracle BLOB with length " + blob.length());
				}
			}
			else {
				ps.setBlob(paramIndex, (Blob) null);
				logger.debug("Set Oracle BLOB to null");
			}
		}

		public void setBlobAsBinaryStream(
				PreparedStatement ps, int paramIndex, final InputStream binaryStream, int contentLength)
				throws SQLException {

			if (binaryStream != null) {
				Blob blob = (Blob) createLob(ps, false, new LobCallback() {
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getBinaryOutputStream", (Class[]) null);
						OutputStream out = (OutputStream) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(binaryStream, out);
					}
				});
				ps.setBlob(paramIndex, blob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set binary stream for Oracle BLOB with length " + blob.length());
				}
			}
			else {
				ps.setBlob(paramIndex, (Blob) null);
				logger.debug("Set Oracle BLOB to null");
			}
		}

		public void setClobAsString(PreparedStatement ps, int paramIndex, final String content)
		    throws SQLException {

			if (content != null) {
				Clob clob = (Clob) createLob(ps, true, new LobCallback() {
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getCharacterOutputStream", (Class[]) null);
						Writer writer = (Writer) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(content, writer);
					}
				});
				ps.setClob(paramIndex, clob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set string for Oracle CLOB with length " + clob.length());
				}
			}
			else {
				ps.setClob(paramIndex, (Clob) null);
				logger.debug("Set Oracle CLOB to null");
			}
		}

		public void setClobAsAsciiStream(
				PreparedStatement ps, int paramIndex, final InputStream asciiStream, int contentLength)
		    throws SQLException {

			if (asciiStream != null) {
				Clob clob = (Clob) createLob(ps, true, new LobCallback() {
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getAsciiOutputStream", (Class[]) null);
						OutputStream out = (OutputStream) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(asciiStream, out);
					}
				});
				ps.setClob(paramIndex, clob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set ASCII stream for Oracle CLOB with length " + clob.length());
				}
			}
			else {
				ps.setClob(paramIndex, (Clob) null);
				logger.debug("Set Oracle CLOB to null");
			}
		}

		public void setClobAsCharacterStream(
				PreparedStatement ps, int paramIndex, final Reader characterStream, int contentLength)
		    throws SQLException {

			if (characterStream != null) {
				Clob clob = (Clob) createLob(ps, true, new LobCallback() {
					public void populateLob(Object lob) throws Exception {
						Method methodToInvoke = lob.getClass().getMethod("getCharacterOutputStream", (Class[]) null);
						Writer writer = (Writer) methodToInvoke.invoke(lob, (Object[]) null);
						FileCopyUtils.copy(characterStream, writer);
					}
				});
				ps.setClob(paramIndex, clob);
				if (logger.isDebugEnabled()) {
					logger.debug("Set character stream for Oracle CLOB with length " + clob.length());
				}
			}
			else {
				ps.setClob(paramIndex, (Clob) null);
				logger.debug("Set Oracle CLOB to null");
			}
		}

		/**
		 * Create a LOB instance for the given PreparedStatement,
		 * populating it via the given callback.
		 */
		protected Object createLob(PreparedStatement ps, boolean clob, LobCallback callback)
				throws SQLException {

			Connection con = null;
			try {
				con = getOracleConnection(ps);
				initOracleDriverClasses(con);
				Object lob = prepareLob(con, clob ? clobClass : blobClass);
				callback.populateLob(lob);
				lob.getClass().getMethod("close", (Class[]) null).invoke(lob, (Object[]) null);
				this.createdLobs.add(lob);
				if (logger.isDebugEnabled()) {
					logger.debug("Created new Oracle " + (clob ? "CLOB" : "BLOB"));
				}
				return lob;
			}
			catch (SQLException ex) {
				throw ex;
			}
			catch (InvocationTargetException ex) {
				if (ex.getTargetException() instanceof SQLException) {
					throw (SQLException) ex.getTargetException();
				}
				else if (con != null && ex.getTargetException() instanceof ClassCastException) {
					throw new InvalidDataAccessApiUsageException(
							"OracleLobCreator needs to work on [oracle.jdbc.OracleConnection], not on [" +
							con.getClass().getName() + "]: specify a corresponding NativeJdbcExtractor",
							ex.getTargetException());
				}
				else {
					throw new DataAccessResourceFailureException("Could not create Oracle LOB",
							ex.getTargetException());
				}
			}
			catch (Exception ex) {
				throw new DataAccessResourceFailureException("Could not create Oracle LOB", ex);
			}
		}

		/**
		 * Retrieve the underlying OracleConnection, using a NativeJdbcExtractor if set.
		 */
		protected Connection getOracleConnection(PreparedStatement ps)
				throws SQLException, ClassNotFoundException {

			return (nativeJdbcExtractor != null ?
					nativeJdbcExtractor.getNativeConnectionFromStatement(ps) : ps.getConnection());
		}

		/**
		 * Create and open an oracle.sql.BLOB/CLOB instance via reflection.
		 */
		protected Object prepareLob(Connection con, Class<?> lobClass) throws Exception {
			/*
			BLOB blob = BLOB.createTemporary(con, false, BLOB.DURATION_SESSION);
			blob.open(BLOB.MODE_READWRITE);
			return blob;
			*/
			Method createTemporary = lobClass.getMethod(
					"createTemporary", Connection.class, boolean.class, int.class);
			Object lob = createTemporary.invoke(null, con, cache, durationSessionConstants.get(lobClass));
			Method open = lobClass.getMethod("open", int.class);
			open.invoke(lob, modeReadWriteConstants.get(lobClass));
			return lob;
		}

		/**
		 * Free all temporary BLOBs and CLOBs created by this creator.
		 */
		public void close() {
			try {
				for (Iterator<Object> it = this.createdLobs.iterator(); it.hasNext();) {
					/*
					BLOB blob = (BLOB) it.next();
					blob.freeTemporary();
					*/
					Object lob = it.next();
					Method freeTemporary = lob.getClass().getMethod("freeTemporary");
					freeTemporary.invoke(lob);
					it.remove();
				}
			}
			catch (InvocationTargetException ex) {
				logger.error("Could not free Oracle LOB", ex.getTargetException());
			}
			catch (Exception ex) {
				throw new DataAccessResourceFailureException("Could not free Oracle LOB", ex);
			}
		}
	}


	/**
	 * Internal callback interface for use with createLob.
	 */
	protected static interface LobCallback {

		/**
		 * Populate the given BLOB or CLOB instance with content.
		 * @throws Exception any exception including InvocationTargetException
		 */
		void populateLob(Object lob) throws Exception;
	}

}
