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

package org.springframework.test.annotation;

import org.springframework.util.Assert;

/**
 * Implementation of {@link ProfileValueSource} which uses system properties as
 * the underlying source.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 */
public final class SystemProfileValueSource implements ProfileValueSource {

	private static final SystemProfileValueSource INSTANCE = new SystemProfileValueSource();


	/**
	 * Obtain the canonical instance of this ProfileValueSource.
	 */
	public static final SystemProfileValueSource getInstance() {
		return INSTANCE;
	}


	/**
	 * Private constructor, enforcing the singleton pattern.
	 */
	private SystemProfileValueSource() {
	}

	/**
	 * Get the <em>profile value</em> indicated by the specified key from the
	 * system properties.
	 * @see System#getProperty(String)
	 */
	@Override
	public String get(String key) {
		Assert.hasText(key, "'key' must not be empty");
		return System.getProperty(key);
	}

}
