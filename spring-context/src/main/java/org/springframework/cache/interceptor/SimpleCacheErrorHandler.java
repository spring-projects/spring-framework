/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.interceptor;

import org.springframework.cache.Cache;

/**
 * A simple {@link CacheErrorHandler} that does not handle the
 * exception at all, simply throwing it back at the client.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class SimpleCacheErrorHandler implements CacheErrorHandler {

	@Override
	public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
		throw exception;
	}

	@Override
	public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
		throw exception;
	}

	@Override
	public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
		throw exception;
	}

	@Override
	public void handleCacheClearError(RuntimeException exception, Cache cache) {
		throw exception;
	}
}
