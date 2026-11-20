/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.annotation;

import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} for Jakarta annotations and their pre-Jakarta equivalents.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
class JakartaAnnotationsRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		Stream.of(
				"jakarta.inject.Inject",
				"jakarta.inject.Provider",
				"jakarta.inject.Qualifier"
			).forEach(typeName -> hints.reflection().registerType(TypeReference.of(typeName)));
	}

}
