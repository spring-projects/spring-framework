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

package org.springframework.jdbc.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * JDBC {@link javax.sql.DataSource} implementation that delegates all calls
 * to a given target {@link javax.sql.DataSource}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #getConnection()}) that should not simply
 * delegate to the target DataSource.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #getConnection
 */
public class DelegatingDataSource implements DataSource, InitializingBean {

	private DataSource targetDataSource;


	/**
	 * Create a new DelegatingDataSource.
	 * @see #setTargetDataSource
	 */
	public DelegatingDataSource() {
	}

	/**
	 * Create a new DelegatingDataSource.
	 * @param targetDataSource the target DataSource
	 */
	public DelegatingDataSource(DataSource targetDataSource) {
		setTargetDataSource(targetDataSource);
	}


	/**
	 * Set the target DataSource that this DataSource should delegate to.
	 */
	public void setTargetDataSource(DataSource targetDataSource) {
		Assert.notNull(targetDataSource, "'targetDataSource' must not be null");
		this.targetDataSource = targetDataSource;
	}

	/**
	 * Return the target DataSource that this DataSource should delegate to.
	 */
	public DataSource getTargetDataSource() {
		return this.targetDataSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (getTargetDataSource() == null) {
			throw new IllegalArgumentException("Property 'targetDataSource' is required");
		}
	}


	@Override
	public Connection getConnection() throws SQLException {
		return getTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return getTargetDataSource().getConnection(username, password);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return getTargetDataSource().getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		getTargetDataSource().setLogWriter(out);
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return getTargetDataSource().getLoginTimeout();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		getTargetDataSource().setLoginTimeout(seconds);
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.0's Wrapper interface
	//---------------------------------------------------------------------

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return getTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || getTargetDataSource().isWrapperFor(iface));
	}


	//---------------------------------------------------------------------
	// Implementation of JDBC 4.1's getParentLogger method
	//---------------------------------------------------------------------

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

}
