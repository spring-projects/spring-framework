/*
 * Copyright 2010-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.util.Assert;

/**
 * Composite {@link CacheOperationSource} implementation that iterates
 * over a given array of {@link CacheOperationSource} instances.
 * 
 * @author Costin Leau
 */
@SuppressWarnings("serial")
public class CompositeCacheOperationSource implements CacheOperationSource, Serializable {

	private final CacheOperationSource[] cacheDefinitionSources;

	/**
	 * Create a new CompositeCachingDefinitionSource for the given sources.
	 * @param cacheDefinitionSourcess the CacheDefinitionSource instances to combine
	 */
	public CompositeCacheOperationSource(CacheOperationSource[] cacheDefinitionSources) {
		Assert.notNull(cacheDefinitionSources, "cacheDefinitionSource array must not be null");
		this.cacheDefinitionSources = cacheDefinitionSources;
	}

	/**
	 * Return the CacheDefinitionSource instances that this
	 * CompositeCachingDefinitionSource combines.
	 */
	public final CacheOperationSource[] getCacheDefinitionSources() {
		return this.cacheDefinitionSources;
	}


	public CacheOperation getCacheOperation(Method method, Class<?> targetClass) {
		for (CacheOperationSource source : cacheDefinitionSources) {
			CacheOperation definition = source.getCacheOperation(method, targetClass);
			if (definition != null) {
				return definition;
			}
		}

		return null;
	}
}