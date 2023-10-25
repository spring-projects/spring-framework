package org.springframework.jdbc.core;

import java.sql.SQLException;
import java.sql.ShardingKey;

import org.springframework.lang.Nullable;

/**
 * Interface defines methods for retrieving sharding keys, which are used to establish
 * direct shard connections (in the context of sharded databases). This is used as a
 * way of providing the sharding key in
 * {@link org.springframework.jdbc.datasource.ShardingKeyDataSourceAdapter}.
 *
 * @author Mohamed Lahyane (Anir)
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
