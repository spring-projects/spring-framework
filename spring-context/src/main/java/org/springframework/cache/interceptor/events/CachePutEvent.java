/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.interceptor.events;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheAspectSupport;

/**
 * Event published after a cache entry has been created or updated.
 *
 * @author Anıl Şenocak
 * @since 7.1
 * @see CacheOperationEvent
 * @see CacheAspectSupport
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
