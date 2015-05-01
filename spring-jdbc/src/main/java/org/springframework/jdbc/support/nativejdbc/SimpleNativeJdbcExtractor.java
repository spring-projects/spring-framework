/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jdbc.support.nativejdbc;

/**
 * A simple implementation of the {@link NativeJdbcExtractor} interface.
 * Assumes a pool that wraps Connection handles but not DatabaseMetaData:
 * In this case, the underlying native Connection can be retrieved by simply
 * calling {@code conHandle.getMetaData().getConnection()}.
 * All other JDBC objects will be returned as passed in.
 *
 * <p>This extractor should work with any pool that does not wrap DatabaseMetaData,
 * and will also work with any plain JDBC driver. Note that a pool can still wrap
 * Statements, PreparedStatements, etc: The only requirement of this extractor is
 * that {@code java.sql.DatabaseMetaData} does not get wrapped, returning the
 * native Connection of the JDBC driver on {@code metaData.getConnection()}.
 *
 * <p>Customize this extractor by setting the "nativeConnectionNecessaryForXxx"
 * flags accordingly: If Statements, PreparedStatements, and/or CallableStatements
 * are wrapped by your pool, set the corresponding "nativeConnectionNecessaryForXxx"
 * flags to "true". If none of the statement types is wrapped - or you solely need
 * Connection unwrapping in the first place -, the defaults are fine.
 *
 * <p>SimpleNativeJdbcExtractor is a common choice for use with OracleLobHandler, which
 * just needs Connection unwrapping via the {@link #getNativeConnectionFromStatement}
 * method. This usage will work with almost any connection pool.
 *
 * <p>For full usage with JdbcTemplate, i.e. to also provide Statement unwrapping:
 * <ul>
 * <li>Use a default SimpleNativeJdbcExtractor for Resin and SJSAS (no JDBC
 * Statement objects are wrapped, therefore no special unwrapping is necessary).
 * <li>Use a SimpleNativeJdbcExtractor with all "nativeConnectionNecessaryForXxx"
 * flags set to "true" for C3P0 (all JDBC Statement objects are wrapped,
 * but none of the wrappers allow for unwrapping).
 * <li>Use a CommonsDbcpNativeJdbcExtractor for Apache Commons DBCP or a
 * JBossNativeJdbcExtractor for JBoss (all JDBC Statement objects are wrapped,
 * but all of them can be extracted by casting to implementation classes).
 * </ul>
 *
 * @author Juergen Hoeller
 * @since 05.12.2003
 * @see #setNativeConnectionNecessaryForNativeStatements
 * @see #setNativeConnectionNecessaryForNativePreparedStatements
 * @see #setNativeConnectionNecessaryForNativeCallableStatements
 * @see Jdbc4NativeJdbcExtractor
 * @see org.springframework.jdbc.core.JdbcTemplate#setNativeJdbcExtractor
 * @see org.springframework.jdbc.support.lob.OracleLobHandler#setNativeJdbcExtractor
 */
public class SimpleNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private boolean nativeConnectionNecessaryForNativeStatements = false;

	private boolean nativeConnectionNecessaryForNativePreparedStatements = false;

	private boolean nativeConnectionNecessaryForNativeCallableStatements = false;


	/**
	 * Set whether it is necessary to work on the native Connection to
	 * receive native Statements. Default is "false". If true, the Connection
	 * will be unwrapped first to create a Statement.
	 * <p>This makes sense if you need to work with native Statements from
	 * a pool that does not allow to extract the native JDBC objects from its
	 * wrappers but returns the native Connection on DatabaseMetaData.getConnection.
	 * <p>The standard SimpleNativeJdbcExtractor is unable to unwrap statements,
	 * so set this to true if your connection pool wraps Statements.
	 * @see java.sql.Connection#createStatement
	 * @see java.sql.DatabaseMetaData#getConnection
	 */
	public void setNativeConnectionNecessaryForNativeStatements(boolean nativeConnectionNecessaryForNativeStatements) {
		this.nativeConnectionNecessaryForNativeStatements = nativeConnectionNecessaryForNativeStatements;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return this.nativeConnectionNecessaryForNativeStatements;
	}

	/**
	 * Set whether it is necessary to work on the native Connection to
	 * receive native PreparedStatements. Default is "false". If true,
	 * the Connection will be unwrapped first to create a PreparedStatement.
	 * <p>This makes sense if you need to work with native PreparedStatements from
	 * a pool that does not allow to extract the native JDBC objects from its
	 * wrappers but returns the native Connection on Statement.getConnection.
	 * <p>The standard SimpleNativeJdbcExtractor is unable to unwrap statements,
	 * so set this to true if your connection pool wraps PreparedStatements.
	 * @see java.sql.Connection#prepareStatement
	 * @see java.sql.DatabaseMetaData#getConnection
	 */
	public void setNativeConnectionNecessaryForNativePreparedStatements(boolean nativeConnectionNecessary) {
		this.nativeConnectionNecessaryForNativePreparedStatements = nativeConnectionNecessary;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return this.nativeConnectionNecessaryForNativePreparedStatements;
	}

	/**
	 * Set whether it is necessary to work on the native Connection to
	 * receive native CallableStatements. Default is "false". If true,
	 * the Connection will be unwrapped first to create a CallableStatement.
	 * <p>This makes sense if you need to work with native CallableStatements from
	 * a pool that does not allow to extract the native JDBC objects from its
	 * wrappers but returns the native Connection on Statement.getConnection.
	 * <p>The standard SimpleNativeJdbcExtractor is unable to unwrap statements,
	 * so set this to true if your connection pool wraps CallableStatements.
	 * @see java.sql.Connection#prepareCall
	 * @see java.sql.DatabaseMetaData#getConnection
	 */
	public void setNativeConnectionNecessaryForNativeCallableStatements(boolean nativeConnectionNecessary) {
		this.nativeConnectionNecessaryForNativeCallableStatements = nativeConnectionNecessary;
	}

	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return this.nativeConnectionNecessaryForNativeCallableStatements;
	}

}
