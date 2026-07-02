package org.springframework.cache.interceptor.events;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;

/**
 * Base class for cache operation events that are published when a cache entry is created, updated, or evicted.
 *
 * @author Anıl Şenocak
 * @see CachePutEvent
 * @see CacheEvictEvent
 * @see CacheClearEvent
 * @see CacheAspectSupport
 * @since 7.1
 */
@SuppressWarnings("serial")
public abstract class CacheOperationEvent extends ApplicationEvent {

	private final Method method;

	private final Cache cache;

	private final @Nullable Object key;

	/**
	 * Create a new {@code CacheOperationEvent}.
	 *
	 * @param method the method that triggered the cache operation
	 * @param cache  the cache on which the operation was performed
	 * @param key    the cache key, or {@code null} for cache-wide clear operations
	 */
	protected CacheOperationEvent(Method method, Cache cache, @Nullable Object key) {
		super(method);
		this.method = method;
		this.cache = cache;
		this.key = key;
	}

	/**
	 * Return the method that triggered the cache operation.
	 */
	public Method getMethod() {
		return this.method;
	}

	/**
	 * Return the cache on which the operation was performed.
	 */
	public Cache getCache() {
		return this.cache;
	}

	/**
	 * Return the cache key that was affected, or {@code null}
	 * for cache-wide clear operations.
	 */
	public @Nullable Object getKey() {
		return this.key;
	}

	/**
	 * Return the cache name the operation was performed on.
	 */
	public String getCacheName() {
		return this.cache.getName();
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append(": ").append(ClassUtils.getQualifiedMethodName(this.method));
		sb.append(" on cache '").append(getCacheName()).append("'");
		if (this.key != null) {
			sb.append(" [").append(this.key).append("]");
		}
		return sb.toString();
	}

}
