/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;
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
	public @Nullable String getString(String name) {
		return this.attributes.get(name);
	}


	private static void assertNotInAotRuntime() {
		if (AotDetector.useGeneratedArtifacts()) {
			throw new UnsupportedOperationException(
				"AOT attributes cannot be modified during AOT run-time execution");
		}
	}

}
