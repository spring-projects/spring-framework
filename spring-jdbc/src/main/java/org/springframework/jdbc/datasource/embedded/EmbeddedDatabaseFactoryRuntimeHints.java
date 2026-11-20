/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.embedded;

import java.util.Collections;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection hints
 * for {@code EmbeddedDataSourceProxy#shutdown} in order to allow it to be used
 * as a bean destroy method.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
class EmbeddedDatabaseFactoryRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection().registerTypeIfPresent(classLoader,
				"org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseFactory$EmbeddedDataSourceProxy",
				builder -> builder
						.onReachableType(EmbeddedDatabaseFactory.class)
						.withMethod("shutdown", Collections.emptyList(), ExecutableMode.INVOKE));
	}

}
