/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.context.support;

import org.springframework.core.style.ToStringCreator;
import org.springframework.test.context.BootstrapContext;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.util.Assert;

/**
 * Default implementation of the {@link BootstrapContext} interface.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class DefaultBootstrapContext implements BootstrapContext {

	private final Class<?> testClass;
	private final CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate;


	/**
	 * Construct a new {@code DefaultBootstrapContext} from the supplied arguments.
	 * @param testClass the test class for this bootstrap context; never {@code null}
	 * @param cacheAwareContextLoaderDelegate the context loader delegate to use for
	 * transparent interaction with the {@code ContextCache}; never {@code null}
	 */
	public DefaultBootstrapContext(Class<?> testClass, CacheAwareContextLoaderDelegate cacheAwareContextLoaderDelegate) {
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
		.append("testClass", this.testClass.getName())//
		.append("cacheAwareContextLoaderDelegate", this.cacheAwareContextLoaderDelegate.getClass().getName())//
		.toString();
	}

}
