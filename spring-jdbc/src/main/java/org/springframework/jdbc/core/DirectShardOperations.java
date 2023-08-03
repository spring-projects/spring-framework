package org.springframework.jdbc.core;

import java.sql.ShardingKey;

import org.springframework.lang.Nullable;

/**
 * Interface specifies direct JDBC data access operations implemented as callbacks.
 * It is intended to be used by database access components and is implemented by
 * {@link DirectShardCallbackTemplate}. While not typically used directly, it offers
 * a valuable option to enhance testability as it can be easily mocked or stubbed.
 *
 * <p>executes JDBC data access operations within a given {@link DirectShardCallback}
 * or {@link Runnable} action on the shard corresponding to the specified sharding key
 * or both sharding key and super sharding key.</p>
 *
 * <p>Allows for returning a result object created within the callback,
 * such as a domain object or a collection of domain objects.</p>
 *
 * <p>
 * <b>Note:</b> For cases where no result object is required, {@link #execute(ShardingKey, Runnable)}
 * can be used to execute queries on the specified shard without returning a result.
 * </p>
 *
 * @author Mohamed Lahyane (Anir)
 * @see ShardingKey
 * @see DirectShardCallback
 * @see DirectShardCallbackTemplate
 */
public interface DirectShardOperations {

	/**
	 * Executes the JDBC data access operations within the given {@link DirectShardCallback} action
	 * on the shard corresponding to the specified sharding key.
	 *
	 * <p>Allows for returning a result object created within the callback, that is,
	 * a domain object or a collection of domain objects.</p>
	 *
	 * @param shardingKey the sharding key that determines the target shard.
	 * @param action      the callback object that specifies the action.
	 * @param <T>         the type of the result object.
	 * @return a result object returned by the callback, or {@code null} if none.
	 * @throws RuntimeException if thrown by the action
	 * @see #execute(ShardingKey, ShardingKey, DirectShardCallback)
	 */
	@Nullable
	default <T> T execute(ShardingKey shardingKey, DirectShardCallback<T> action) {
		return execute(shardingKey, null, action);
	}

	/**
	 * Executes the JDBC data access operations within the given {@link DirectShardCallback} action
	 * on the shard corresponding to the specified sharding key and super sharding key.
	 *
	 * <p>Allows for returning a result object created within the callback, that is,
	 * a domain object or a collection of domain objects.</p>
	 *
	 * @param shardingKey       the sharding key that determines the target shard.
	 * @param superShardingKey  the additional sharding key that is used to determine the target shard.
	 * @param action            the callback object that specifies the action.
	 * @param <T>               the type of the result object.
	 * @return a result object returned by the callback, or {@code null} if none.
	 * @throws RuntimeException if thrown by the action
	 * @see #execute(ShardingKey, ShardingKey, DirectShardCallback)
	 */
	@Nullable
	<T> T execute(ShardingKey shardingKey, @Nullable ShardingKey superShardingKey, DirectShardCallback<T> action);

	/**
	 * Executes the JDBC data access operations within the given {@link Runnable} action
	 * on the shard corresponding to the specified sharding key, without returning a result.
	 *
	 * @param shardingKey the sharding key that determines the target shard.
	 * @param action      the callback object that specifies the action.
	 * @throws RuntimeException if thrown by the action
	 * @see #execute(ShardingKey, ShardingKey, Runnable)
	 */
	default void execute(ShardingKey shardingKey, Runnable action) {
		execute(shardingKey, null, action);
	}

	/**
	 * Executes the JDBC data access operations within the given {@link Runnable} action
	 * on the shard corresponding to the specified sharding key and super sharding key,
	 * without returning a result.
	 *
	 * @param shardingKey       the sharding key that determines the target shard.
	 * @param superShardingKey  the additional sharding key that is used to determine the target shard.
	 * @param action            the callback object that specifies the action.
	 * @throws RuntimeException if thrown by the action
	 */
	default void execute(ShardingKey shardingKey, @Nullable ShardingKey superShardingKey, Runnable action) {
		execute(shardingKey, superShardingKey, () -> {
			action.run();
			return null;
		});
	}
}