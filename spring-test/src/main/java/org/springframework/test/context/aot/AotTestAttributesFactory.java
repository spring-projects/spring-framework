/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.aot;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.AotDetector;

/**
 * Factory for {@link AotTestAttributes}.
 *
 * @author Sam Brannen
 * @since 6.0
 */
final class AotTestAttributesFactory {

	private static volatile @Nullable Map<String, String> attributes;


	private AotTestAttributesFactory() {
	}

	/**
	 * Get the underlying attributes map.
	 * <p>If the map is not already loaded, this method loads the map from the
	 * generated class when running in {@linkplain AotDetector#useGeneratedArtifacts()
	 * AOT execution mode} and otherwise creates a new map for storing attributes
	 * during the AOT processing phase.
	 */
	static Map<String, String> getAttributes() {
		Map<String, String> attrs = attributes;
		if (attrs == null) {
			synchronized (AotTestAttributesFactory.class) {
				attrs = attributes;
				if (attrs == null) {
					attrs = (AotDetector.useGeneratedArtifacts() ? loadAttributesMap() : new ConcurrentHashMap<>());
					attributes = attrs;
				}
			}
		}
		return attrs;
	}

	/**
	 * Reset the factory.
	 * <p>Only for internal use.
	 */
	static void reset() {
		synchronized (AotTestAttributesFactory.class) {
			attributes = null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> loadAttributesMap() {
		String className = AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_CLASS_NAME;
		String methodName = AotTestAttributesCodeGenerator.GENERATED_ATTRIBUTES_METHOD_NAME;
		return GeneratedMapUtils.loadMap(className, methodName);
	}

}
