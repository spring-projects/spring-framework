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

package org.springframework.orm.hibernate3;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;

import org.hibernate.HibernateException;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.util.JDBCExceptionReporter;

import org.springframework.jdbc.datasource.DataSourceUtils;

/**
 * Hibernate connection provider for local DataSource instances
 * in an application context. This provider will be used if
 * LocalSessionFactoryBean's "dataSource" property is set
 * without a Hibernate TransactionManagerLookup.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see LocalSessionFactoryBean#setDataSource
 * @deprecated as of Spring 4.3, in favor of Hibernate 4.x/5.x
 */
@Deprecated
public class LocalDataSourceConnectionProvider implements ConnectionProvider {

	private DataSource dataSource;

	private DataSource dataSourceToUse;


	@Override
	public void configure(Properties props) throws HibernateException {
		this.dataSource = LocalSessionFactoryBean.getConfigTimeDataSource();
		// absolutely needs thread-bound DataSource to initialize
		if (this.dataSource == null) {
			throw new HibernateException("No local DataSource found for configuration - " +
				"'dataSource' property must be set on LocalSessionFactoryBean");
		}
		this.dataSourceToUse = getDataSourceToUse(this.dataSource);
	}

	/**
	 * Return the DataSource to use for retrieving Connections.
	 * <p>This implementation returns the passed-in DataSource as-is.
	 * @param originalDataSource the DataSource as configured by the user
	 * on LocalSessionFactoryBean
	 * @return the DataSource to actually retrieve Connections from
	 * (potentially wrapped)
	 * @see LocalSessionFactoryBean#setDataSource
	 */
	protected DataSource getDataSourceToUse(DataSource originalDataSource) {
		return originalDataSource;
	}

	/**
	 * Return the DataSource that this ConnectionProvider wraps.
	 */
	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * This implementation delegates to the underlying DataSource.
	 * @see javax.sql.DataSource#getConnection()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		try {
			return this.dataSourceToUse.getConnection();
		}
		catch (SQLException ex) {
			JDBCExceptionReporter.logExceptions(ex);
			throw ex;
		}
	}

	/**
	 * This implementation calls {@link DataSourceUtils#doCloseConnection},
	 * checking against a {@link org.springframework.jdbc.datasource.SmartDataSource}.
	 */
	@Override
	public void closeConnection(Connection con) throws SQLException {
		try {
			DataSourceUtils.doCloseConnection(con, this.dataSourceToUse);
		}
		catch (SQLException ex) {
			JDBCExceptionReporter.logExceptions(ex);
			throw ex;
		}
	}

	/**
	 * This implementation does nothing:
	 * We're dealing with an externally managed DataSource.
	 */
	@Override
	public void close() {
	}

	/**
	 * This implementation returns {@code false}: We cannot guarantee
	 * to receive the same Connection within a transaction, not even when
	 * dealing with a JNDI DataSource.
	 */
	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

}
