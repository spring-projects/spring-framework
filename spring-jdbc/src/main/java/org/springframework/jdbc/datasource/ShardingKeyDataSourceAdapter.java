package org.springframework.jdbc.datasource;

import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;
import java.sql.ShardingKeyBuilder;

import javax.sql.DataSource;

import org.springframework.core.NamedThreadLocal;
import org.springframework.jdbc.core.ShardingKeyProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An adapter for a target {@link DataSource}, designed to apply sharding keys, if specified,
 * to every standard {@code getConnection()} call, returning a direct connection to the shard
 * corresponding to the specified sharding key value. All other methods are simply delegated
 * to the corresponding methods of the target DataSource.
 *
 * <p>The target {@link DataSource} must implement the {@code createConnectionBuilder()} method;
 * otherwise, a {@link java.sql.SQLFeatureNotSupportedException} will be thrown when attempting
 * to acquire shard connections.</p>
 *
 * <p>This proxy datasource takes a {@link ShardingKeyProvider} object as an attribute,
 * which is used to get the sharding keys.</p>
 *
 * @author Mohamed Lahyane (Anir)
 * @see #getConnection
 * @see #createConnectionBuilder()
 * @see UserCredentialsDataSourceAdapter
 */
public class ShardingKeyDataSourceAdapter extends DelegatingDataSource {
	@Nullable
	private ShardingKeyProvider shardingkeyProvider;

	/**
	 * Creates a new instance of ShardingKeyDataSourceAdapter, wrapping the given {@link DataSource}.
	 *
	 * @param dataSource the target DataSource to be wrapped.
	 */
	public ShardingKeyDataSourceAdapter(DataSource dataSource) {
		super(dataSource);
	}

	/**
	 * Creates a new instance of ShardingKeyDataSourceAdapter, wrapping the given {@link DataSource}.
	 *
	 * @param dataSource the target DataSource to be wrapped.
	 * @param shardingKeyProvider the ShardingKeyProvider used to get the shardingKeys.
	 */
	public ShardingKeyDataSourceAdapter(DataSource dataSource, ShardingKeyProvider shardingKeyProvider) {
		super(dataSource);
		this.shardingkeyProvider = shardingKeyProvider;
	}

	/**
	 * Sets the {@link ShardingKeyProvider} for this adapter.
	 *
	 * @param shardingKeyProvider the ShardingKeyProvider to set.
	 */
	public void setShardingKeyProvider(ShardingKeyProvider shardingKeyProvider) {
		this.shardingkeyProvider = shardingKeyProvider;
	}

	/**
	 * Obtains a connection to the database shard using the provided sharding key
	 * and super sharding key (if available).
	 * <p>the sharding key is obtained from the thread local storage, if is {@code null},
	 * it is obtained from the {@link ShardingKeyProvider}.</p>
	 *
	 * @return a Connection object representing a direct shard connection.
	 * @throws SQLException if an error occurs while creating the connection.
	 * @see #createConnectionBuilder()
	 */
	@Override
	public Connection getConnection() throws SQLException {
		return createConnectionBuilder().build();
	}

	/**
	 * Obtains a connection to the database shard using the provided username and password,
	 * considering the sharding keys (if available) and the given credentials.
	 *
	 * @param username the database user on whose behalf the connection is being made.
	 * @param password the user's password.
	 * @return a Connection object representing a direct shard connection.
	 * @throws SQLException if an error occurs while creating the connection.
	 */
	@Override
	public Connection getConnection(String username, String password) throws SQLException {
		return createConnectionBuilder().user(username).password(password).build();
	}

	/**
	 * Creates a new instance of {@link ConnectionBuilder} using the target DataSource's
	 * {@code createConnectionBuilder()} method, and sets the appropriate sharding keys
	 * from the thread-local storage or the {@link ShardingKeyProvider}.
	 *
	 * @return a ConnectionBuilder object representing a builder for direct shard connections.
	 * @throws SQLException if an error occurs while creating the ConnectionBuilder.
	 */
	@Override
	public ConnectionBuilder createConnectionBuilder() throws SQLException {
		ConnectionBuilder connectionBuilder = obtainTargetDataSource().createConnectionBuilder();

		ShardingKey shardingKey = null;
		ShardingKey superShardingKey = null;

		if (shardingkeyProvider != null) {
			shardingKey = shardingkeyProvider.getShardingKey();
			superShardingKey = shardingkeyProvider.getSuperShardingKey();
		}

		return connectionBuilder.shardingKey(shardingKey).superShardingKey(superShardingKey);
	}

	/**
	 * Creates a new instance of {@link ShardingKeyBuilder} using the target DataSource's
	 * {@code createShardingKeyBuilder()} method.
	 *
	 * @return a ShardingKeyBuilder object representing a builder for sharding keys.
	 * @throws SQLException if an error occurs while creating the ShardingKeyBuilder.
	 */
	@Override
	public ShardingKeyBuilder createShardingKeyBuilder() throws SQLException {
		return obtainTargetDataSource().createShardingKeyBuilder();
	}
}
