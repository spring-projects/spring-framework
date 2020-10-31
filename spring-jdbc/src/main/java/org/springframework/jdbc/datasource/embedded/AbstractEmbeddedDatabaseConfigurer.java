/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.jdbc.datasource.embedded;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for {@link EmbeddedDatabaseConfigurer} implementations
 * providing common shutdown behavior through a "SHUTDOWN" statement.
 *
 * @author Oliver Gierke
 * @author Juergen Hoeller
 * @since 3.0
 */
abstract class AbstractEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	protected final Log logger = LogFactory.getLog(getClass());


	@Override
	public void shutdown(DataSource dataSource, String databaseName) {
		Connection con = null;
		try {
			con = dataSource.getConnection();
			if (con != null) {
				try (Statement stmt = con.createStatement()) {
					stmt.execute("SHUTDOWN");
				}
			}
		}
		catch (SQLException ex) {
			logger.info("Could not shut down embedded database", ex);
		}
		finally {
			if (con != null) {
				try {
					con.close();
				}
				catch (SQLException ex) {
					logger.debug("Could not close JDBC Connection on shutdown", ex);
				}
				catch (Throwable ex) {
					logger.debug("Unexpected exception on closing JDBC Connection", ex);
				}
			}
		}
	}

}
