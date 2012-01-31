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

package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Simple {@link ConfigurableEnvironment} implementation exposing a
 * {@link #setProperty(String, String)} and {@link #withProperty(String, String)}
 * methods for testing purposes.
 * 
 * @author Chris Beams
 * @since 3.1
 * @see MockPropertySource
 */
public class MockEnvironment extends AbstractEnvironment {

	private MockPropertySource propertySource = new MockPropertySource();

	/**
	 * Create a new {@code MockEnvironment} with a single {@link MockPropertySource}.
	 */
	public MockEnvironment() {
		getPropertySources().addLast(propertySource);
	}

	/**
	 * Set a property on the underlying {@link MockPropertySource} for this environment.
	 */
	public void setProperty(String key, String value) {
		propertySource.setProperty(key, value);
	}

	/**
	 * Convenient synonym for {@link #setProperty} that returns the current instance.
	 * Useful for method chaining and fluent-style use.
	 * @return this {@link MockEnvironment} instance
	 * @see MockPropertySource#withProperty(String, String)
	 */
	public MockEnvironment withProperty(String key, String value) {
		this.setProperty(key, value);
		return this;
	}

}
