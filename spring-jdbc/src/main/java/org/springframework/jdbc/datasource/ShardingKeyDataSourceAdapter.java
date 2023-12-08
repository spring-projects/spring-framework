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

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

import javax.sql.DataSource;

import org.springframework.lang.Nullable;

/**
 * An adapter for a target {@link DataSource}, designed to apply sharding keys, if specified,
 * to every standard {@code #getConnection} call, returning a direct connection to the shard
 * corresponding to the specified sharding key value. All other methods are simply delegated
 * to the corresponding methods of the target DataSource.
 *
 * <p>The target {@link DataSource} must implement the {@link #createConnectionBuilder} method;
 * otherwise, a {@link java.sql.SQLFeatureNotSupportedException} will be thrown when attempting
 * to acquire shard connections.
 *
 * <p>This adapter needs to be configured with a {@link ShardingKeyProvider} callback which is
 * used to get the current sharding keys for every {@code #getConnection} call, for example:
 *
 * <pre class="code">
 * ShardingKeyDataSourceAdapter dataSourceAdapter = new ShardingKeyDataSourceAdapter(dataSource);
 * dataSourceAdapter.setShardingKeyProvider(() -> dataSource.createShardingKeyBuilder()
 *     .subkey(SecurityContextHolder.getContext().getAuthentication().getName(), JDBCType.VARCHAR).build());
 * </pre>
 *
 * @author Mohamed Lahyane (Anir)
 * @author Juergen Hoeller
 * @since 6.1.2
 * @see #getConnection
 * @see #createConnectionBuilder()
 * @see UserCredentialsDataSourceAdapter
 */
public class ShardingKeyDataSourceAdapter extends DelegatingDataSource {

	@Nullable
	private ShardingKeyProvider shardingkeyProvider;


	/**
	 * Create a new instance of ShardingKeyDataSourceAdapter, wrapping the given {@link DataSource}.
	 * @param dataSource the target DataSource to be wrapped
	 */
	public ShardingKeyDataSourceAdapter(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Create a new instance of ShardingKeyDataSourceAdapter, wrapping the given {@link DataSource}.
	 * @param dataSource the target DataSource to be wrapped
	 * @param shardingKeyProvider the ShardingKeyProvider used to get the sharding keys
	 */
	public ShardingKeyDataSourceAdapter(DataSource dataSource, ShardingKeyProvider shardingKeyProvider) {
		super(dataSource);
		this.shardingkeyProvider = shardingKeyProvider;
	}


	/**
	 * Set the {@link ShardingKeyProvider} for this adapter.
	 */
	public void setShardingKeyProvider(ShardingKeyProvider shardingKeyProvider) {
		this.shardingkeyProvider = shardingKeyProvider;
	}


	/**
	 * Obtain a connection to the database shard using the provided sharding key
	 * and super sharding key (if available).
	 * <p>The sharding key is obtained from the {@link ShardingKeyProvider}.
	 * @return a Connection object representing a direct shard connection
	 * @throws SQLException if an error occurs while creating the connection
	 * @see #createConnectionBuilder()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return createConnectionBuilder().build();
	}

	/**
	 * Obtain a connection to the database shard using the provided username and password,
	 * considering the sharding keys (if available) and the given credentials.
	 * <p>The sharding key is obtained from the {@link ShardingKeyProvider}.
	 * @param username the database user on whose behalf the connection is being made
	 * @param password the user's password
	 * @return a Connection object representing a direct shard connection.
	 * @throws SQLException if an error occurs while creating the connection.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return createConnectionBuilder().user(username).password(password).build();
	}

	/**
	 * Create a new instance of {@link ConnectionBuilder} using the target DataSource's
	 * {@code createConnectionBuilder()} method, and sets the appropriate sharding keys
	 * from the {@link ShardingKeyProvider}.
	 * @return a ConnectionBuilder object representing a builder for direct shard connections
	 * @throws SQLException if an error occurs while creating the ConnectionBuilder
	 */
	@Override
	public ConnectionBuilder createConnectionBuilder() throws SQLException {
		ConnectionBuilder connectionBuilder = super.createConnectionBuilder();
		if (this.shardingkeyProvider == null) {
			return connectionBuilder;
		}

		ShardingKey shardingKey = this.shardingkeyProvider.getShardingKey();
		ShardingKey superShardingKey = this.shardingkeyProvider.getSuperShardingKey();
		return connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey);
	}

}
