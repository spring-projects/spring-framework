/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.aot;

import java.util.Map;

import org.springframework.aot.AotDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link AotTestAttributes} backed by a {@link Map}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
class DefaultAotTestAttributes implements AotTestAttributes {

	private final Map<String, String> attributes;


	DefaultAotTestAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}


	@Override
	public void setAttribute(String name, String value) {
		assertNotInAotRuntime();
		Assert.notNull(value, "'value' must not be null");
		Assert.isTrue(!this.attributes.containsKey(name),
				() -> "AOT attributes cannot be overridden. Name '%s' is already in use.".formatted(name));
		this.attributes.put(name, value);
	}

	@Override
	public void removeAttribute(String name) {
		assertNotInAotRuntime();
		this.attributes.remove(name);
	}

	@Override
	@Nullable
	public String getString(String name) {
		return this.attributes.get(name);
	}


	private static void assertNotInAotRuntime() {
		if (AotDetector.useGeneratedArtifacts()) {
			throw new UnsupportedOperationException(
				"AOT attributes cannot be modified during AOT run-time execution");
		}
	}

}
