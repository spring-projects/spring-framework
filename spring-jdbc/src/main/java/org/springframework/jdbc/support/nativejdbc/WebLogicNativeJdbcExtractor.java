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

package org.springframework.jdbc.support.nativejdbc;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.util.ReflectionUtils;

/**
 * Implementation of the {@link NativeJdbcExtractor} interface for WebLogic,
 * supporting WebLogic Server 8.1 and higher.
 *
 * <p>Returns the underlying native Connection to application code instead
 * of WebLogic's wrapper implementation; unwraps the Connection for native
 * statements. The returned JDBC classes can then safely be cast, e.g. to
 * {@code oracle.jdbc.OracleConnection}.
 *
 * <p>This NativeJdbcExtractor can be set just to <i>allow</i> working
 * with a WebLogic DataSource: If a given object is not a WebLogic
 * Connection wrapper, it will be returned as-is.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see #getNativeConnection
 * @see weblogic.jdbc.extensions.WLConnection#getVendorConnection
 */
public class WebLogicNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private static final String JDBC_EXTENSION_NAME = "weblogic.jdbc.extensions.WLConnection";


	private final Class jdbcExtensionClass;

	private final Method getVendorConnectionMethod;


	/**
	 * This constructor retrieves the WebLogic JDBC extension interface,
	 * so we can get the underlying vendor connection using reflection.
	 */
	public WebLogicNativeJdbcExtractor() {
		try {
			this.jdbcExtensionClass = getClass().getClassLoader().loadClass(JDBC_EXTENSION_NAME);
			this.getVendorConnectionMethod = this.jdbcExtensionClass.getMethod("getVendorConnection", (Class[]) null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebLogicNativeJdbcExtractor because WebLogic API classes are not available: " + ex);
		}
	}


	/**
	 * Return {@code true}, as WebLogic returns wrapped Statements.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return true;
	}

	/**
	 * Return {@code true}, as WebLogic returns wrapped PreparedStatements.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return true;
	}

	/**
	 * Return {@code true}, as WebLogic returns wrapped CallableStatements.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return true;
	}

	/**
	 * Retrieve the Connection via WebLogic's {@code getVendorConnection} method.
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (this.jdbcExtensionClass.isAssignableFrom(con.getClass())) {
			return (Connection) ReflectionUtils.invokeJdbcMethod(this.getVendorConnectionMethod, con);
		}
		return con;
	}

}
