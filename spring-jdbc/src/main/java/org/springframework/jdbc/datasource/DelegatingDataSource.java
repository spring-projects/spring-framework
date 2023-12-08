/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKeyBuilder;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
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

	@Nullable
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
	public void setTargetDataSource(@Nullable DataSource targetDataSource) {
		this.targetDataSource = targetDataSource;
	}

	/**
	 * Return the target DataSource that this DataSource should delegate to.
	 */
	@Nullable
	public DataSource getTargetDataSource() {
		return this.targetDataSource;
	}

	/**
	 * Obtain the target {@code DataSource} for actual use (never {@code null}).
	 * @since 5.0
	 */
	protected DataSource obtainTargetDataSource() {
		DataSource dataSource = getTargetDataSource();
		Assert.state(dataSource != null, "No 'targetDataSource' set");
		return dataSource;
	}

	@Override
	public void afterPropertiesSet() {
		if (getTargetDataSource() == null) {
			throw new IllegalArgumentException("Property 'targetDataSource' is required");
		}
	}


	@Override
	public Connection getConnection() throws SQLException {
		return obtainTargetDataSource().getConnection();
	}

	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return obtainTargetDataSource().getConnection(username, password);
	}

	@Override
	public ConnectionBuilder createConnectionBuilder() throws SQLException {
		return obtainTargetDataSource().createConnectionBuilder();
	}

	@Override
	public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
		return obtainTargetDataSource().createShardingKeyBuilder();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return obtainTargetDataSource().getLoginTimeout();
	}

	@Override
	public void setLoginTimeout(int seconds) throws SQLException {
		obtainTargetDataSource().setLoginTimeout(seconds);
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return obtainTargetDataSource().getLogWriter();
	}

	@Override
	public void setLogWriter(PrintWriter out) throws SQLException {
		obtainTargetDataSource().setLogWriter(out);
	}

	@Override
	public Logger getParentLogger() {
		return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (iface.isInstance(this)) {
			return (T) this;
		}
		return obtainTargetDataSource().unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return (iface.isInstance(this) || obtainTargetDataSource().isWrapperFor(iface));
	}

}
