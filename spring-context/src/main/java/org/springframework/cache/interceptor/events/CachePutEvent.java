package org.springframework.cache.interceptor.events;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheAspectSupport;

import java.lang.reflect.Method;

/**
 * Event published after a cache entry has been created or updated.
 *
 * @author Anıl Şenocak
 * @see CacheOperationEvent
 * @see CacheAspectSupport
 * @since 7.1
 */
@SuppressWarnings("serial")
public class CachePutEvent extends CacheOperationEvent {
	private final @Nullable Object value;


	/**
	 * Create a new {@code CachePutEvent}.
	 *
	 * @param method the method that triggered the cache operation
	 * @param cache  the cache on which the entry was put
	 * @param key    the cache key
	 * @param value  the value that was cached
	 */
	public CachePutEvent(Method method, Cache cache, Object key, @Nullable Object value) {
		super(method, cache, key);
		this.value = value;
	}


	/**
	 * Return the value that was placed into the cache.
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

}
