/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.SQLException;
import java.sql.ShardingKey;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for determining sharding keys which are used to establish direct
 * shard connections in the context of sharded databases. This is used as a callback
 * for providing the current sharding key (plus optionally a super sharding key) in
 * {@link org.springframework.jdbc.datasource.ShardingKeyDataSourceAdapter}.
 *
 * <p>Can be used as a functional interface (for example, with a lambda expression) for a simple
 * sharding key, or as a two-method interface when including a super sharding key as well.
 *
 * @author Mohamed Lahyane (Anir)
 * @author Juergen Hoeller
 * @since 6.1.2
 * @see ShardingKeyDataSourceAdapter#setShardingKeyProvider
 */
public interface ShardingKeyProvider {

	/**
	 * Determine the sharding key. This method returns the sharding key relevant to the current
	 * context which will be used to obtain a direct shard connection.
	 * @return the sharding key, or {@code null} if it is not available or cannot be determined
	 * @throws SQLException if an error occurs while obtaining the sharding key
	 */
	@Nullable ShardingKey getShardingKey() throws SQLException;

	/**
	 * Determine the super sharding key, if any. This method returns the super sharding key
	 * relevant to the current context which will be used to obtain a direct shard connection.
	 * @return the super sharding key, or {@code null} if it is not available or cannot be
	 * determined (the default)
	 * @throws SQLException if an error occurs while obtaining the super sharding key
	 */
	default @Nullable ShardingKey getSuperShardingKey() throws SQLException {
		return null;
	}

}
