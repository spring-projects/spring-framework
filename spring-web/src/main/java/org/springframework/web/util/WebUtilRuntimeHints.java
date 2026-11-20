/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers resource hints
 * for resources in the {@code web.util} package.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class WebUtilRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.resources().registerResource(
				new ClassPathResource("HtmlCharacterEntityReferences.properties", getClass()));
	}

}
