/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;

/**
 * Utility methods for executing a DatabasePopulator.
 *
 * @author Juergen Hoeller
 * @since 3.1
 */
public abstract class DatabasePopulatorUtils {

	/**
	 * Execute the given DatabasePopulator against the given DataSource.
	 * @param populator the DatabasePopulator to execute
	 * @param dataSource the DataSource to execute against
	 */
	public static void execute(DatabasePopulator populator, DataSource dataSource) {
		Assert.notNull(populator, "DatabasePopulator must be provided");
		Assert.notNull(dataSource, "DataSource must be provided");
		try {
			Connection connection = dataSource.getConnection();
			try {
				populator.populate(connection);
			}
			finally {
				try {
					connection.close();
				}
				catch (SQLException ex) {
					// ignore
				}
			}
		}
		catch (Exception ex) {
			throw new DataAccessResourceFailureException("Failed to execute database script", ex);
		}
	}

}
