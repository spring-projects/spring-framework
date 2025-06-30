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

package org.springframework.cache.jcache.config;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * An extension of {@link CachingConfigurerSupport} that also implements
 * {@link JCacheConfigurer}.
 *
 * <p>Users of JSR-107 annotations may extend from this class rather than
 * implementing from {@link JCacheConfigurer} directly.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JCacheConfigurer
 * @see CachingConfigurerSupport
 * @deprecated as of 6.0 in favor of implementing {@link JCacheConfigurer} directly
 */
@Deprecated(since = "6.0")
public class JCacheConfigurerSupport extends CachingConfigurerSupport implements JCacheConfigurer {

	@Override
	public @Nullable CacheResolver exceptionCacheResolver() {
		return null;
	}

}
