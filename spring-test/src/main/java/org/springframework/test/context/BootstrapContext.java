/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.test.context;

/**
 * {@code BootstrapContext} encapsulates the context in which the <em>Spring
 * TestContext Framework</em> is bootstrapped.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapWith
 * @see TestContextBootstrapper
 */
public interface BootstrapContext {

	/**
	 * Get the {@linkplain Class test class} for this bootstrap context.
	 * @return the test class (never {@code null})
	 */
	Class<?> getTestClass();

	/**
	 * Get the {@link CacheAwareContextLoaderDelegate} to use for transparent
	 * interaction with the {@code ContextCache}.
	 * @return the context loader delegate (never {@code null})
	 */
	CacheAwareContextLoaderDelegate getCacheAwareContextLoaderDelegate();

}
