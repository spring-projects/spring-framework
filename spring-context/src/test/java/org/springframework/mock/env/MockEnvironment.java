/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.testfixture.env.MockPropertySource;

/**
 * Simple {@link ConfigurableEnvironment} implementation exposing
 * {@link #setProperty} and {@link #withProperty} methods for testing purposes.
 *
 * @author Chris Beams
 * @author Sam Brannen
 * @since 3.2
 * @see MockPropertySource
 */
public class MockEnvironment extends AbstractEnvironment {

	private final MockPropertySource propertySource = new MockPropertySource();


	/**
	 * Create a new {@code MockEnvironment} with a single {@link MockPropertySource}.
	 */
	public MockEnvironment() {
		getPropertySources().addLast(this.propertySource);
	}


	/**
	 * Set a property on the underlying {@link MockPropertySource} for this environment.
	 * @since 6.2.8
	 * @see MockPropertySource#setProperty(String, Object)
	 */
	public void setProperty(String name, Object value) {
		this.propertySource.setProperty(name, value);
	}

	/**
	 * Convenient synonym for {@link #setProperty(String, Object)} that returns
	 * the current instance.
	 * <p>Useful for method chaining and fluent-style use.
	 * @return this {@link MockEnvironment} instance
	 * @since 6.2.8
	 * @see MockPropertySource#withProperty(String, Object)
	 */
	public MockEnvironment withProperty(String name, Object value) {
		setProperty(name, value);
		return this;
	}

}
