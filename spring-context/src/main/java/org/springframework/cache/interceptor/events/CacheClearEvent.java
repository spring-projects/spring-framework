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

import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheAspectSupport;

/**
 * Event published after a cache has been entirely cleared.
 *
 * @author Anıl Şenocak
 * @since 7.1
 * @see CacheOperationEvent
 * @see CacheAspectSupport
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
