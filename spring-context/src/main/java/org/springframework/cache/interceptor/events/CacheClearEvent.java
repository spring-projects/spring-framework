package org.springframework.cache.interceptor.events;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheAspectSupport;

import java.lang.reflect.Method;

/**
 * Event published after a cache has been entirely cleared.
 *
 * @author Anıl Şenocak
 * @see CacheOperationEvent
 * @see CacheAspectSupport
 * @since 7.1
 */
@SuppressWarnings("serial")
public class CacheClearEvent extends CacheOperationEvent {

	/**
	 * Create a new {@code CacheClearEvent}.
	 *
	 * @param method the method that triggered the cache operation
	 * @param cache  the cache that was cleared
	 */
	public CacheClearEvent(Method method, Cache cache) {
		super(method, cache, null);
	}

}
