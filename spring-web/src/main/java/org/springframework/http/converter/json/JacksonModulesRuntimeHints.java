/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.converter.json;

import java.util.function.Consumer;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeHint.Builder;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers reflection entries
 * for {@link Jackson2ObjectMapperBuilder} well-known modules.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
class JacksonModulesRuntimeHints implements RuntimeHintsRegistrar {

	private static final Consumer<Builder> asJacksonModule = builder ->
			builder.onReachableType(Jackson2ObjectMapperBuilder.class)
					.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		hints.reflection()
				.registerTypeIfPresent(classLoader,
						"com.fasterxml.jackson.datatype.jdk8.Jdk8Module", asJacksonModule)
				.registerTypeIfPresent(classLoader,
						"com.fasterxml.jackson.datatype.jsr310.JavaTimeModule", asJacksonModule)
				.registerTypeIfPresent(classLoader,
						"com.fasterxml.jackson.module.kotlin.KotlinModule", asJacksonModule);
	}

}
