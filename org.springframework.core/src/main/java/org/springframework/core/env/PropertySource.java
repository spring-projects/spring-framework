/*
 * Copyright 2002-2010 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public abstract class PropertySource<T> {

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected final String name;
	protected final T source;

	public PropertySource(String name, T source) {
		this.name = name;
		this.source = source;
	}

	public String getName() {
		return name;
	}

	public T getSource() {
		return source;
	}

	public abstract boolean containsProperty(String key);

	public abstract String getProperty(String key);

	public abstract int size();


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof PropertySource))
			return false;
		PropertySource<?> other = (PropertySource<?>) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Produce concise output (type, name, and number of properties) if the current log level does
	 * not include debug. If debug is enabled, produce verbose output including hashcode of the
	 * PropertySource instance and every key/value property pair.
	 *
	 * This variable verbosity is useful as a property source such as system properties
	 * or environment variables may contain an arbitrary number of property pairs, potentially
	 * leading to difficult to read exception and log messages.
	 *
	 * @see Log#isDebugEnabled()
	 */
	@Override
	public String toString() {
		if (logger.isDebugEnabled()) {
			return String.format("%s@%s [name='%s', properties=%s]",
					getClass().getSimpleName(), System.identityHashCode(this), name, source);
		}

		return String.format("%s [name='%s', propertyCount=%d]",
				getClass().getSimpleName(), name, this.size());
	}


	/**
	 * For collection comparison purposes
	 * TODO SPR-7508: document
	 */
	public static PropertySource<?> named(String name) {
		return new ComparisonPropertySource(name);
	}


	/**
	 * TODO: SPR-7508: document
	 */
	public static class ComparisonPropertySource extends PropertySource<Void>{

		private static final String USAGE_ERROR =
			"ComparisonPropertySource instances are for collection comparison " +
			"use only";

		public ComparisonPropertySource(String name) {
			super(name, null);
		}

		@Override
		public Void getSource() {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
		public String getProperty(String key) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
		public boolean containsProperty(String key) {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}
		public int size() {
			throw new UnsupportedOperationException(USAGE_ERROR);
		}

		@Override
		public String toString() {
			return String.format("%s [name='%s']", getClass().getSimpleName(), name);
		}
	}
}
