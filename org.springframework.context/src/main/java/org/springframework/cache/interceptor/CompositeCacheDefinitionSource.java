/*
 * Copyright 2010 the original author or authors.
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
 * Composite {@link CacheDefinitionSource} implementation that iterates
 * over a given array of {@link CacheDefinitionSource} instances.
 * 
 * @author Costin Leau
 */
@SuppressWarnings("serial")
public class CompositeCacheDefinitionSource implements CacheDefinitionSource, Serializable {

	private final CacheDefinitionSource[] cacheDefinitionSources;

	/**
	 * Create a new CompositeCachingDefinitionSource for the given sources.
	 * @param cacheDefinitionSourcess the CacheDefinitionSource instances to combine
	 */
	public CompositeCacheDefinitionSource(CacheDefinitionSource[] cacheDefinitionSources) {
		Assert.notNull(cacheDefinitionSources, "cacheDefinitionSource array must not be null");
		this.cacheDefinitionSources = cacheDefinitionSources;
	}

	/**
	 * Return the CacheDefinitionSource instances that this
	 * CompositeCachingDefinitionSource combines.
	 */
	public final CacheDefinitionSource[] getCacheDefinitionSources() {
		return this.cacheDefinitionSources;
	}


	public CacheDefinition getCacheDefinition(Method method, Class<?> targetClass) {
		for (CacheDefinitionSource source : cacheDefinitionSources) {
			CacheDefinition definition = source.getCacheDefinition(method, targetClass);
			if (definition != null) {
				return definition;
			}
		}

		return null;
	}
}
