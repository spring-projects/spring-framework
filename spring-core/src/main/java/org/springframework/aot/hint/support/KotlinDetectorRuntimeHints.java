/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.support;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} to register hints for {@link org.springframework.core.KotlinDetector}.
 *
 * @author Brian Clozel
 * @since 6.1
 */
class KotlinDetectorRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerType(TypeReference.of("kotlin.Metadata"))
				.registerType(TypeReference.of("kotlin.reflect.full.KClasses"));
	}
}
