package org.springframework.jdbc.core;

import java.sql.SQLException;
import java.sql.ShardingKey;

import org.springframework.lang.Nullable;

/**
 * Interface defines methods for retrieving sharding keys, which are used to establish
 * direct shard connections (in the context of sharded databases). This is used as a
 * way of providing the sharding key, besides the thread-bound sharding keys in
 * {@link org.springframework.jdbc.datasource.ShardingKeyDataSourceAdapter}.
 *
 * <p>It is particularly useful in scenarios where using {@link DirectShardCallbackTemplate} is not
 * practical, like when the sharding key is independent of the specific query being executed.
 * For instance, the sharding key may depend on an HTTP session (authenticated user's data).</p>
 *
 * <p>In cases where used in parallel with {@link DirectShardCallbackTemplate}, the sharding key
 * provided in the {@link DirectShardCallbackTemplate} method is the one that will be used, and
 * not the one provided by {@link ShardingKeyProvider}.</p>
 *
 * @author Mohamed Lahyane (Anir)
 * @see DirectShardCallbackTemplate
 */


public interface ShardingKeyProvider {
	/**
	 * Retrieves the sharding key. This method returns the sharding key relevant to the current context,
	 * which will be used to obtain a direct shard connection.
	 *
	 * @return The sharding key, or null if it is not available or cannot be determined.
	 * @throws SQLException If an error occurs while obtaining the sharding key.
	 */
	@Nullable
	ShardingKey getShardingKey() throws SQLException;

	/**
	 * Retrieves the super sharding key. This method returns the super sharding key relevant to the
	 * current context, which will be used to obtain a direct shard connection.
	 *
	 * @return The super sharding key, or null if it is not available or cannot be determined.
	 * @throws SQLException If an error occurs while obtaining the super sharding key.
	 */
	@Nullable
	ShardingKey getSuperShardingKey() throws SQLException;
}
