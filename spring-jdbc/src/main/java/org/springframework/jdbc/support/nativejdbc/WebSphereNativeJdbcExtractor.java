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
 * Implementation of the {@link NativeJdbcExtractor} interface for WebSphere,
 * supporting WebSphere Application Server 5.1 and higher.
 *
 * <p>Returns the underlying native Connection to application code instead
 * of WebSphere's wrapper implementation; unwraps the Connection for
 * native statements. The returned JDBC classes can then safely be cast,
 * e.g. to {@code oracle.jdbc.OracleConnection}.
 *
 * <p>This NativeJdbcExtractor can be set just to <i>allow</i> working
 * with a WebSphere DataSource: If a given object is not a WebSphere
 * Connection wrapper, it will be returned as-is.
 *
 * @author Juergen Hoeller
 * @since 1.1
 */
public class WebSphereNativeJdbcExtractor extends NativeJdbcExtractorAdapter {

	private static final String JDBC_ADAPTER_CONNECTION_NAME_5 = "com.ibm.ws.rsadapter.jdbc.WSJdbcConnection";

	private static final String JDBC_ADAPTER_UTIL_NAME_5 = "com.ibm.ws.rsadapter.jdbc.WSJdbcUtil";


	private Class webSphere5ConnectionClass;

	private Method webSphere5NativeConnectionMethod;


	/**
	 * This constructor retrieves WebSphere JDBC adapter classes,
	 * so we can get the underlying vendor connection using reflection.
	 */
	public WebSphereNativeJdbcExtractor() {
		try {
			this.webSphere5ConnectionClass = getClass().getClassLoader().loadClass(JDBC_ADAPTER_CONNECTION_NAME_5);
			Class jdbcAdapterUtilClass = getClass().getClassLoader().loadClass(JDBC_ADAPTER_UTIL_NAME_5);
			this.webSphere5NativeConnectionMethod =
					jdbcAdapterUtilClass.getMethod("getNativeConnection", new Class[] {this.webSphere5ConnectionClass});
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Could not initialize WebSphereNativeJdbcExtractor because WebSphere API classes are not available: " + ex);
		}
	}


	/**
	 * Return {@code true}, as WebSphere returns wrapped Statements.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeStatements() {
		return true;
	}

	/**
	 * Return {@code true}, as WebSphere returns wrapped PreparedStatements.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativePreparedStatements() {
		return true;
	}

	/**
	 * Return {@code true}, as WebSphere returns wrapped CallableStatements.
	 */
	@Override
	public boolean isNativeConnectionNecessaryForNativeCallableStatements() {
		return true;
	}

	/**
	 * Retrieve the Connection via WebSphere's {@code getNativeConnection} method.
	 */
	@Override
	protected Connection doGetNativeConnection(Connection con) throws SQLException {
		if (this.webSphere5ConnectionClass.isAssignableFrom(con.getClass())) {
			return (Connection) ReflectionUtils.invokeJdbcMethod(
					this.webSphere5NativeConnectionMethod, null, new Object[] {con});
		}
		return con;
	}

}
