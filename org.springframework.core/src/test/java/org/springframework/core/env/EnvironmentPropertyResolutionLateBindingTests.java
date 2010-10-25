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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;


/**
 * Test that  {@link Environment#getValue} performs late-resolution of property
 * values i.e., does not eagerly resolve and cache only at construction time.
 *
 * @see EnvironmentPropertyResolutionSearchTests
 * @author Chris Beams
 * @since 3.1
 */
public class EnvironmentPropertyResolutionLateBindingTests {
	@Test
	public void replaceExistingKeyPostConstruction() {
		String key = "foo";
		String value1 = "bar";
		String value2 = "biz";

		System.setProperty(key, value1); // before construction
		DefaultEnvironment env = new DefaultEnvironment();
		assertThat(env.getProperty(key), equalTo(value1));
		System.setProperty(key, value2); // after construction and first resolution
		assertThat(env.getProperty(key), equalTo(value2));
		System.clearProperty(key); // clean up
	}

	@Test
	public void addNewKeyPostConstruction() {
		DefaultEnvironment env = new DefaultEnvironment();
		assertThat(env.getProperty("foo"), equalTo(null));
		System.setProperty("foo", "42");
		assertThat(env.getProperty("foo"), equalTo("42"));
		System.clearProperty("foo"); // clean up
	}
}
