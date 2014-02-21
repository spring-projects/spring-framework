/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Base class for cache operations.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 */
public abstract class CacheOperation {

	private Set<String> cacheNames = Collections.emptySet();

	private String condition = "";

	private String key = "";

	private String keyGenerator = "";

	private String cacheManager = "";

	private String name = "";


	public Set<String> getCacheNames() {
		return cacheNames;
	}

	public String getCondition() {
		return condition;
	}

	public String getKey() {
		return key;
	}

	public String getKeyGenerator() {
		return keyGenerator;
	}

	public String getCacheManager() {
		return cacheManager;
	}

	public String getName() {
		return name;
	}

	public void setCacheName(String cacheName) {
		Assert.hasText(cacheName);
		this.cacheNames = Collections.singleton(cacheName);
	}

	public void setCacheNames(String[] cacheNames) {
		Assert.notEmpty(cacheNames);
		this.cacheNames = new LinkedHashSet<String>(cacheNames.length);
		for (String string : cacheNames) {
			this.cacheNames.add(string);
		}
	}

	public void setCondition(String condition) {
		Assert.notNull(condition);
		this.condition = condition;
	}

	public void setKey(String key) {
		Assert.notNull(key);
		this.key = key;
	}

	public void setKeyGenerator(String keyGenerator) {
		Assert.notNull(keyGenerator);
		this.keyGenerator = keyGenerator;
	}

	public void setCacheManager(String cacheManager) {
		Assert.notNull(cacheManager);
		this.cacheManager = cacheManager;
	}

	public void setName(String name) {
		Assert.hasText(name);
		this.name = name;
	}

	/**
	 * This implementation compares the {@code toString()} results.
	 * @see #toString()
	 */
	@Override
	public boolean equals(Object other) {
		return (other instanceof CacheOperation && toString().equals(other.toString()));
	}

	/**
	 * This implementation returns {@code toString()}'s hash code.
	 * @see #toString()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Return an identifying description for this cache operation.
	 * <p>Has to be overridden in subclasses for correct {@code equals}
	 * and {@code hashCode} behavior. Alternatively, {@link #equals}
	 * and {@link #hashCode} can be overridden themselves.
	 */
	@Override
	public String toString() {
		return getOperationDescription().toString();
	}

	/**
	 * Return an identifying description for this caching operation.
	 * <p>Available to subclasses, for inclusion in their {@code toString()} result.
	 */
	protected StringBuilder getOperationDescription() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append("[");
		result.append(this.name);
		result.append("] caches=");
		result.append(this.cacheNames);
		result.append(" | key='");
		result.append(this.key);
		result.append("' | keyGenerator='");
		result.append(this.keyGenerator);
		result.append("' | cacheManager='");
		result.append(this.cacheManager);
		result.append("' | condition='");
		result.append(this.condition);
		result.append("'");
		return result;
	}
}
