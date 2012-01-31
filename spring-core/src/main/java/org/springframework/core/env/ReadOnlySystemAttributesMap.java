/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.core.env;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.util.Assert;

/**
 * Read-only {@code Map<String, String>} implementation that is backed by system properties or environment
 * variables.
 *
 * <p>Used by {@link AbstractApplicationContext} when a {@link SecurityManager} prohibits access to {@link
 * System#getProperties()} or {@link System#getenv()}.
 *
 * @author Arjen Poutsma
 * @author Chris Beams
 * @since 3.0
 */
abstract class ReadOnlySystemAttributesMap implements Map<String, String> {

	public boolean containsKey(Object key) {
		return get(key) != null;
	}

	/**
	 * @param key the name of the system attribute to retrieve
	 * @throws IllegalArgumentException if given key is non-String
	 */
	public String get(Object key) {
		Assert.isInstanceOf(String.class, key,
			String.format("expected key [%s] to be of type String, got %s",
					key, key.getClass().getName()));

		return this.getSystemAttribute((String) key);
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * Template method that returns the underlying system attribute.
	 *
	 * <p>Implementations typically call {@link System#getProperty(String)} or {@link System#getenv(String)} here.
	 */
	protected abstract String getSystemAttribute(String attributeName);

	// Unsupported

	public int size() {
		throw new UnsupportedOperationException();
	}

	public String put(String key, String value) {
		throw new UnsupportedOperationException();
	}

	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	public String remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public Set<String> keySet() {
		throw new UnsupportedOperationException();
	}

	public void putAll(Map<? extends String, ? extends String> m) {
		throw new UnsupportedOperationException();
	}

	public Collection<String> values() {
		throw new UnsupportedOperationException();
	}

	public Set<Entry<String, String>> entrySet() {
		throw new UnsupportedOperationException();
	}

}
