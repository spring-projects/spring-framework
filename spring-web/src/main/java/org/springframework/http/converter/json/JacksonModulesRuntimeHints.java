/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.converter.json;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint.Builder;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection hints
 * for {@link Jackson2ObjectMapperBuilder} well-known modules.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
class JacksonModulesRuntimeHints implements RuntimeHintsRegistrar {

	@SuppressWarnings("removal")
	private static final Consumer<Builder> asJacksonModule = builder ->
			builder.onReachableType(Jackson2ObjectMapperBuilder.class)
					.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.reflection()
				.registerTypeIfPresent(classLoader,
						"com.fasterxml.jackson.datatype.jdk8.Jdk8Module", asJacksonModule)
				.registerTypeIfPresent(classLoader,
						"com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", asJacksonModule)
				.registerTypeIfPresent(classLoader,
						"com.fasterxml.jackson.module.kotlin.KotlinModule", asJacksonModule);
	}

}
