/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.lang.Nullable;

/**
 * Bean that checks if a database has already started up. To be referenced
 * via "depends-on" from beans that depend on database startup, like a Hibernate
 * SessionFactory or custom data access objects that access a DataSource directly.
 *
 * <p>Useful to defer application initialization until a database has started up.
 * Particularly appropriate for waiting on a slowly starting Oracle database.
 *
 * @author Juergen Hoeller
 * @since 18.12.2003
 */
public class DatabaseStartupValidator implements InitializingBean {

	/**
	 * The default interval.
	 */
	public static final int DEFAULT_INTERVAL = 1;

	/**
	 * The default timeout.
	 */
	public static final int DEFAULT_TIMEOUT = 60;


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private DataSource dataSource;

	@Nullable
	private String validationQuery;

	private int interval = DEFAULT_INTERVAL;

	private int timeout = DEFAULT_TIMEOUT;


	/**
	 * Set the DataSource to validate.
	 */
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Set the SQL query string to use for validation.
	 */
	public void setValidationQuery(String validationQuery) {
		this.validationQuery = validationQuery;
	}

	/**
	 * Set the interval between validation runs (in seconds).
	 * Default is {@value #DEFAULT_INTERVAL}.
	 */
	public void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * Set the timeout (in seconds) after which a fatal exception
	 * will be thrown. Default is {@value #DEFAULT_TIMEOUT}.
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}


	/**
	 * Check whether the validation query can be executed on a Connection
	 * from the specified DataSource, with the specified interval between
	 * checks, until the specified timeout.
	 */
	@Override
	public void afterPropertiesSet() {
		if (this.dataSource == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (this.validationQuery == null) {
			throw new IllegalArgumentException("Property 'validationQuery' is required");
		}

		try {
			boolean validated = false;
			long beginTime = System.currentTimeMillis();
			long deadLine = beginTime + TimeUnit.SECONDS.toMillis(this.timeout);
			SQLException latestEx = null;

			while (!validated && System.currentTimeMillis() < deadLine) {
				Connection con = null;
				Statement stmt = null;
				try {
					con = this.dataSource.getConnection();
					if (con == null) {
						throw new CannotGetJdbcConnectionException("Failed to execute validation query: " +
								"DataSource returned null from getConnection(): " + this.dataSource);
					}
					stmt = con.createStatement();
					stmt.execute(this.validationQuery);
					validated = true;
				}
				catch (SQLException ex) {
					latestEx = ex;
					if (logger.isDebugEnabled()) {
						logger.debug("Validation query [" + this.validationQuery + "] threw exception", ex);
					}
					if (logger.isWarnEnabled()) {
						float rest = ((float) (deadLine - System.currentTimeMillis())) / 1000;
						if (rest > this.interval) {
							logger.warn("Database has not started up yet - retrying in " + this.interval +
									" seconds (timeout in " + rest + " seconds)");
						}
					}
				}
				finally {
					JdbcUtils.closeStatement(stmt);
					JdbcUtils.closeConnection(con);
				}

				if (!validated) {
					TimeUnit.SECONDS.sleep(this.interval);
				}
			}

			if (!validated) {
				throw new CannotGetJdbcConnectionException(
						"Database has not started up within " + this.timeout + " seconds", latestEx);
			}

			if (logger.isInfoEnabled()) {
				float duration = ((float) (System.currentTimeMillis() - beginTime)) / 1000;
				logger.info("Database startup detected after " + duration + " seconds");
			}
		}
		catch (InterruptedException ex) {
			// Re-interrupt current thread, to allow other threads to react.
			Thread.currentThread().interrupt();
		}
	}

}
