package org.springframework.jdbc.core;


import org.springframework.lang.Nullable;

/**
 * Callback interface for code that contains JDBC data access operations to be
 * executed directly on a database shard. Used with {@link DirectShardCallbackTemplate}'s
 * {@code execute} method.
 *
 * @author Mohamed Lahyane (Anir)
 * @param <T> The result type
 * @see DirectShardCallbackTemplate
 */

@FunctionalInterface
public interface DirectShardCallback<T> {
	/**
	 * Gets called by {@link DirectShardCallbackTemplate}, all JDBC data access
	 * operations within this method are executed using direct shard connections.
	 * <p>allows for returning a result object created within the callback, i.e. a
	 * domain object or a collection of domain objects.</p>
	 *
	 * @return a result object or {@code null}
	 * @see DirectShardCallbackTemplate#execute
	 */
	@Nullable
	T doInShard();
}