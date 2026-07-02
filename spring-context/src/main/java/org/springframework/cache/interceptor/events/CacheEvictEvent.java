package org.springframework.cache.interceptor.events;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheAspectSupport;

import java.lang.reflect.Method;

/**
 * Event published after a single cache entry has been evicted.
 *
 * @author Anıl Şenocak
 * @see CacheOperationEvent
 * @see CacheAspectSupport
 * @since 7.1
 */
@SuppressWarnings("serial")
public class CacheEvictEvent extends CacheOperationEvent {

	/**
	 * Create a new {@code CacheEvictEvent}.
	 *
	 * @param method the method that triggered the cache operation
	 * @param cache  the cache from which the entry was evicted
	 * @param key    the cache key that was evicted
	 */
	public CacheEvictEvent(Method method, Cache cache, Object key) {
		super(method, cache, key);
	}

}
