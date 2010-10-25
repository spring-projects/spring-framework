/*
 * Copyright 2002-2009 the original author or authors.
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

	public String get(Object key) {
		if (key instanceof String) {
			String attributeName = (String) key;
			return getSystemAttribute(attributeName);
		}
		else {
			// TODO SPR-7508: technically breaks backward-compat. Used to return null
			// for non-string keys, now throws. Any callers who have coded to this
			// behavior will now break.  It's highly unlikely, however; could be
			// a calculated risk to take.  Throwing is a better choice, as returning
			// null represents a 'false negative' - it's not actually that the key
			// isn't present, it's simply that you cannot access it through the current
			// abstraction.  Remember, this case would only come up if (a) there are
			// non-string keys or values in system properties, (b) there is a
			// SecurityManager present, and (c) the user attempts to access one
			// of those properties through this abstraction. This combination is
			// probably unlikely enough to merit the change.
			//
			// note also that the previous implementation didn't consider the
			// possibility of non-string values the anonymous implementation used
			// for System properties access now does.
			//
			// See AbstractEnvironment for relevant anonymous implementations
			// See DefaultEnvironmentTests for unit tests around these cases
			throw new IllegalStateException("TODO SPR-7508: message");
		}
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
