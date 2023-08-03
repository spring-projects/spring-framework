package org.springframework.jdbc.core;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.ShardingKey;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.datasource.ShardingKeyDataSourceAdapter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Template class that simplifies direct JDBC operations execution on database shards.
 *
 * <p>The central method of this class is {@link #execute}. It takes a sharding key and
 * a {@link DirectShardCallback} as parameters. Every database operation within the
 * callback is executed directly on the shard corresponding to the specified sharding key.
 * </p>
 *
 * <p>Typical usage: Allows for low-level data access objects such as {@link JdbcTemplate}
 * that use a sharding-aware DataSource ({@link ShardingKeyDataSourceAdapter}) but
 * are not sharding-aware themselves to run direct shard database operations.
 * Higher-level application services, using this class, can make calls to the low-level
 * services via a callback object {@link DirectShardCallback}.</p>
 *
 * <p>Instances of this class can be used within a service implementation by
 * instantiating it directly with a {@code ShardingKeyDataSourceAdapter} reference.
 * Alternatively, it can be prepared in an application context and passed to
 * services as a bean reference.</p>
 *
 * <p>Note that the {@code ShardingKeyDataSourceAdapter} reference must be the
 * DataSource object used to acquire JDBC shard connections by the data access object
 * used within the callback object.</p>
 *
 * @author Mohamed Lahyane (Anir)
 * @see #execute
 * @see #setShardingKeyDataSourceAdapter
 * @see ShardingKeyDataSourceAdapter
 */
public class DirectShardCallbackTemplate implements DirectShardOperations, InitializingBean {
	@Nullable
	private ShardingKeyDataSourceAdapter shardingKeyDataSourceAdapter;

	/**
	 * Construct a new {@code DirectShardCallbackTemplate} for bean usage.
	 * <p>Note: The ShardingKeyDataSourceAdapter needs to be set before using the instance
	 * @see #setShardingKeyDataSourceAdapter
	 */
	public DirectShardCallbackTemplate() {
	}

	/**
	 * Construct a new {@code DirectShardCallbackTemplate}, given a ShardingKeyDataSourceAdapter.
	 * @param shardingKeyDataSourceAdapter the ShardingKey datasource adapter to be used.
	 */
	public DirectShardCallbackTemplate(ShardingKeyDataSourceAdapter shardingKeyDataSourceAdapter) {
		setShardingKeyDataSourceAdapter(shardingKeyDataSourceAdapter);
	}

	/**
	 * Set the ShardingKeyDataSourceAdapter to be used
	 * @param shardingKeyDataSourceAdapter the ShardingKey datasource adapter to be used.
	 */
	public void setShardingKeyDataSourceAdapter(ShardingKeyDataSourceAdapter shardingKeyDataSourceAdapter) {
		this.shardingKeyDataSourceAdapter = shardingKeyDataSourceAdapter;
		afterPropertiesSet();
	}

	/**
	 * Return the ShardingKeyDataSourceAdapter used by this template.
	 */
	@Nullable
	public ShardingKeyDataSourceAdapter getShardingKeyDataSourceAdapter() {
		return this.shardingKeyDataSourceAdapter;
	}

	@Override
	public <T> T execute(ShardingKey shardingKey, DirectShardCallback<T> action) {
		return execute(shardingKey, null, action);
	}

	@Override
	public <T> T execute(ShardingKey shardingKey, @Nullable ShardingKey superShardingKey, DirectShardCallback<T> action) {
		Assert.notNull(this.shardingKeyDataSourceAdapter, "No ShardingKeyDataSourceAdapter set");
		try {
			this.shardingKeyDataSourceAdapter.setShardingKeyForCurrentThread(shardingKey);
			if (superShardingKey != null) {
				this.shardingKeyDataSourceAdapter.setSuperShardingKeyForCurrentThread(superShardingKey);
			}
			return action.doInShard();
		}
		catch (RuntimeException | Error ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new UndeclaredThrowableException(ex, "ShardingKeyAwareCallback threw undeclared checked exception");
		}
		finally {
			this.shardingKeyDataSourceAdapter.clearShardingKeysFromCurrentThread();
		}
	}

	@Override
	public void afterPropertiesSet() {
		if (this.shardingKeyDataSourceAdapter == null) {
			throw new IllegalArgumentException("Property 'ShardingKeyDataSourceAdapter' is required");
		}
	}
}