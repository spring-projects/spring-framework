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


/**
 * Base class implementing {@link CacheDefinition}.
 *  
 * @author Costin Leau
 */
abstract class AbstractCacheDefinition implements CacheDefinition {

	private String cacheName = "";
	private String condition = "";
	private String key = "";
	private String name = "";


	public String getCacheName() {
		return cacheName;
	}

	public String getCondition() {
		return condition;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * This implementation compares the <code>toString()</code> results.
	 * @see #toString()
	 */
	@Override
	public boolean equals(Object other) {
		return (other instanceof CacheDefinition && toString().equals(other.toString()));
	}

	/**
	 * This implementation returns <code>toString()</code>'s hash code.
	 * @see #toString()
	 */
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Return an identifying description for this cache operation definition.
	 * <p>Has to be overridden in subclasses for correct <code>equals</code>
	 * and <code>hashCode</code> behavior. Alternatively, {@link #equals}
	 * and {@link #hashCode} can be overridden themselves.
	 */
	@Override
	public String toString() {
		return getDefinitionDescription().toString();
	}

	/**
	 * Return an identifying description for this caching definition.
	 * <p>Available to subclasses, for inclusion in their <code>toString()</code> result.
	 */
	protected StringBuilder getDefinitionDescription() {
		StringBuilder result = new StringBuilder();
		result.append(cacheName);
		result.append(',');
		result.append(condition);
		result.append(",");
		result.append(key);

		return result;
	}
}