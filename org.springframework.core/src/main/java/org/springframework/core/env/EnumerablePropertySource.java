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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

/**
 * A {@link PropertySource} implementation capable of interrogating its
 * underlying source object to enumerate all possible property name/value
 * pairs. Exposes the {@link #getPropertyNames()} method to allow callers
 * to introspect available properties without having to access the underlying
 * source object. This also facilitates a more efficient implementation of
 * {@link #containsProperty(String)}, in that it can call {@link #getPropertyNames()}
 * and iterate through the returned array rather than attempting a call to
 * {@link #getProperty(String)} which may be more expensive. Implementations may
 * consider caching the result of {@link #getPropertyNames()} to fully exploit this
 * performance opportunity.
 *
 * Most framework-provided {@code PropertySource} implementations are enumerable;
 * a counter-example would be {@code JndiPropertySource} where, due to the
 * nature of JNDI it is not possible to determine all possible property names at
 * any given time; rather it is only possible to try to access a property
 * (via {@link #getProperty(String)}) in order to evaluate whether it is present
 * or not.
 *
 * @author Chris Beams
 * @since 3.1
 */
public abstract class EnumerablePropertySource<T> extends PropertySource<T> {

	protected static final String[] EMPTY_NAMES_ARRAY = new String[0];

	protected final Log logger = LogFactory.getLog(getClass());


	public EnumerablePropertySource(String name, T source) {
		super(name, source);
	}

	/**
	 * Return the names of all properties contained by the {@linkplain #getSource()
	 * source} object (never {@code null}).
	 */
	public abstract String[] getPropertyNames();

	/**
	 * Return whether this {@code PropertySource} contains a property with the given name.
	 * <p>This implementation checks for the presence of the given name within
	 * the {@link #getPropertyNames()} array.
	 * @param name the property to find
	 */
	public boolean containsProperty(String name) {
		Assert.notNull(name, "property name must not be null");
		for (String candidate : this.getPropertyNames()) {
			if (candidate.equals(name)) {
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("PropertySource [%s] contains '%s'", getName(), name));
				}
				return true;
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("PropertySource [%s] does not contain '%s'", getName(), name));
		}
		return false;
	}

}
