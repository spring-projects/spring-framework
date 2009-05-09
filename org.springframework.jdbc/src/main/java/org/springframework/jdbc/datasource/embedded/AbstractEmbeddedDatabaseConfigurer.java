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

/**
 * Base class for {@link EmbeddedDatabaseConfigurer} implementations providing common shutdown behaviour.
 * @author Oliver Gierke
 */
abstract class AbstractEmbeddedDatabaseConfigurer implements EmbeddedDatabaseConfigurer {

	private static final Log logger = LogFactory.getLog(AbstractEmbeddedDatabaseConfigurer.class);

	public void shutdown(DataSource dataSource, String databaseName) {
		Connection connection = JdbcUtils.getConnection(dataSource);
		Statement stmt = null;
		try {
			stmt = connection.createStatement();
			stmt.execute("SHUTDOWN");
		} catch (SQLException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Could not shutdown embedded database", e);
			}
		} finally {
			JdbcUtils.closeStatement(stmt);
		}
	}
}
