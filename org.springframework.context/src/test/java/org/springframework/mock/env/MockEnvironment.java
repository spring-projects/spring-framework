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

package org.springframework.mock.env;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;


/**
 * Simple {@link ConfigurableEnvironment} implementation exposing a
 * {@link #setProperty(String, String)} and {@link #withProperty(String, String)}
 * methods for testing purposes.
 * 
 * @author Chris Beams
 * @see MockPropertySource
 */
public class MockEnvironment extends AbstractEnvironment {

	private MockPropertySource propertySource = new MockPropertySource();

	public MockEnvironment() {
		getPropertySources().add(propertySource);
	}

	public void setProperty(String key, String value) {
		propertySource.setProperty(key, value);
	}

	public static MockEnvironment withProperty(String key, String value) {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty(key, value);
		return environment;
	}
}
