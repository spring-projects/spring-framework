/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.support;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * {@link RuntimeHintsRegistrar} for {@link PathMatchingResourcePatternResolver}.
 * @author Brian Clozel
 */
class PathMatchingResourcePatternResolverRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerType(TypeReference.of("org.eclipse.core.runtime.FileLocator"));
	}
}
