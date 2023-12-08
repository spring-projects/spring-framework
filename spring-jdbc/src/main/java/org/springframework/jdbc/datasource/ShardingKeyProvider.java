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

import java.sql.SQLException;
import java.sql.ShardingKey;

import org.springframework.lang.Nullable;

/**
 * Strategy interface for determining sharding keys which are used to establish direct
 * shard connections in the context of sharded databases. This is used as a callback
 * for providing the current sharding key (plus optionally a super sharding key) in
 * {@link org.springframework.jdbc.datasource.ShardingKeyDataSourceAdapter}.
 *
 * <p>Can be used as a functional interface (e.g. with a lambda expression) for a simple
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
	@Nullable
	ShardingKey getShardingKey() throws SQLException;

	/**
	 * Determine the super sharding key, if any. This method returns the super sharding key
	 * relevant to the current context which will be used to obtain a direct shard connection.
	 * @return the super sharding key, or {@code null} if it is not available or cannot be
	 * determined (the default)
	 * @throws SQLException if an error occurs while obtaining the super sharding key
	 */
	@Nullable
	default ShardingKey getSuperShardingKey() throws SQLException {
		return null;
	}

}
