/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.support;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.BindingReflectionHintsRegistrar;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} to register hints for popular conventions in
 * {@code org.springframework.core.convert.support.ObjectToObjectConverter}.
 * Some dynamic hints registered by {@link BindingReflectionHintsRegistrar}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 6.0
 */
class ObjectToObjectConverterRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		ReflectionHints reflectionHints = hints.reflection();

		TypeReference sqlDateTypeReference = TypeReference.of("java.sql.Date");
		reflectionHints.registerTypeIfPresent(classLoader, sqlDateTypeReference.getName(), hint -> hint
				.withMethod("toLocalDate", Collections.emptyList(), ExecutableMode.INVOKE)
				.onReachableType(sqlDateTypeReference)
				.withMethod("valueOf", List.of(TypeReference.of(LocalDate.class)), ExecutableMode.INVOKE)
				.onReachableType(sqlDateTypeReference));

		TypeReference sqlTimestampTypeReference = TypeReference.of("java.sql.Timestamp");
		reflectionHints.registerTypeIfPresent(classLoader, sqlTimestampTypeReference.getName(), hint -> hint
				.withMethod("from", List.of(TypeReference.of(Instant.class)), ExecutableMode.INVOKE)
				.onReachableType(sqlTimestampTypeReference));

		reflectionHints.registerTypeIfPresent(classLoader, "org.springframework.http.HttpMethod",
				builder -> builder.withMethod("valueOf", List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE));

		reflectionHints.registerTypeIfPresent(classLoader, "java.net.URI", MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
	}

}
