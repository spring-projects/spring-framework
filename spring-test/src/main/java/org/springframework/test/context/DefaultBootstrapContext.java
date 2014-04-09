/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link BootstrapContext} interface.
 *
 * @author Sam Brannen
 * @since 4.1
 */
class DefaultBootstrapContext implements BootstrapContext {

	private final Class<?> testClass;
	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;


	DefaultBootstrapContext(Class<?> testClass, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
		Assert.notNull(testClass, "Test class must not be null");
		Assert.notNull(cacheAwareContextLoaderDelegate, "CacheAwareContextLoaderDelegate must not be null");
		this.testClass = testClass;
		this.cacheAwareContextLoaderDelegate = cacheAwareContextLoaderDelegate;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getTestClass() {
		return this.testClass;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate() {
		return this.cacheAwareContextLoaderDelegate;
	}

	/**
	 * Provide a String representation of this bootstrap context's state.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this)//
		.append("testClass", testClass)//
		.append("cacheAwareContextLoaderDelegate", nullSafeToString(cacheAwareContextLoaderDelegate))//
		.toString();
	}

	/**
	 * Generate a null-safe {@link String} representation of the supplied
	 * {@link CacheAwareContextLoaderDelegate} based solely on the fully qualified
	 * name of the delegate or &quot;null&quot; if the supplied delegate is
	 * {@code null}.
	 */
	private static String nullSafeToString(CacheAwareContextLoaderDelegate delegate) {
		return delegate == null ? "null" : delegate.getClass().getName();
	}

}
