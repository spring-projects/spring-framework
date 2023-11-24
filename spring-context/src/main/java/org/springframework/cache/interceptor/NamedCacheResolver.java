/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Collection;
import java.util.List;

import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;

/**
 * A {@link CacheResolver} that forces the resolution to a configurable
 * collection of name(s) against a given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class NamedCacheResolver extends AbstractCacheResolver {

	@Nullable
	private Collection<String> cacheNames;


	public NamedCacheResolver() {
	}

	public NamedCacheResolver(CacheManager cacheManager, String... cacheNames) {
		super(cacheManager);
		this.cacheNames = List.of(cacheNames);
	}


	/**
	 * Set the cache name(s) that this resolver should use.
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return this.cacheNames;
	}

}
