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

package org.springframework.aot.hint.support;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} to register hints for popular conventions in
 * {@code org.springframework.core.convert.support.ObjectToObjectConverter}.
 *
 * @author Sebastien Deleuze
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

		reflectionHints.registerTypeIfPresent(classLoader, "org.springframework.http.HttpMethod",
				builder -> builder.withMethod("valueOf", List.of(TypeReference.of(String.class)), ExecutableMode.INVOKE));
		reflectionHints.registerTypeIfPresent(classLoader, "java.net.URI", MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
	}

}
