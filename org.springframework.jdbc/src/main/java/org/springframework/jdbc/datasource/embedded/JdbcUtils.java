/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.jdbc.datasource.embedded;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

/**
 * Helper JDBC utilities used by other classes in this package.
 * Note there is some duplication here with JdbcUtils in jdbc.support package.
 * We may want to consider simply using that at some point.
 * @author Keith Donald
 */
final class JdbcUtils {

	private static Log logger = LogFactory.getLog(EmbeddedDatabaseFactory.class);

	private JdbcUtils() {

	}

	public static Connection getConnection(DataSource dataSource) {
		try {
			return dataSource.getConnection();
		} catch (SQLException ex) {
			throw new CannotGetJdbcConnectionException("Could not get JDBC Connection", ex);
		}
	}

	public static void closeConnection(Connection connection) {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException ex) {
				logger.debug("Could not close JDBC Connection", ex);
			} catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JDBC Connection", ex);
			}
		}
	}

	public static void closeStatement(Statement stmt) {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException ex) {
				logger.debug("Could not close JDBC Statement", ex);
			} catch (Throwable ex) {
				// We don't trust the JDBC driver: It might throw RuntimeException or Error.
				logger.debug("Unexpected exception on closing JDBC Statement", ex);
			}
		}
	}
}
