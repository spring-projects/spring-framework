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

package org.springframework.cache.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * An implementation of {@link CachingConfigurer} with empty methods allowing
 * subclasses to override only the methods they're interested in.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CachingConfigurer
 * @deprecated as of 6.0 in favor of implementing {@link CachingConfigurer} directly
 */
@Deprecated(since = "6.0")
public class CachingConfigurerSupport implements CachingConfigurer {

	@Override
	public @Nullable CacheManager cacheManager() {
		return null;
	}

	@Override
	public @Nullable CacheResolver cacheResolver() {
		return null;
	}

	@Override
	public @Nullable KeyGenerator keyGenerator() {
		return null;
	}

	@Override
	public @Nullable CacheErrorHandler errorHandler() {
		return null;
	}

}
