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

package org.springframework.http.codec;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;
import org.springframework.lang.Nullable;

/**
 * {@link RuntimeHintsRegistrar} implementation that registers runtime hints for
 * implementations listed in {@code CodecConfigurer.properties}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
class CodecConfigurerRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.resources().registerPattern(
				"org/springframework/http/codec/CodecConfigurer.properties");
		hints.reflection().registerTypes(
				TypeReference.listOf(DefaultClientCodecConfigurer.class, DefaultServerCodecConfigurer.class),
				typeHint -> typeHint.onReachableType(CodecConfigurerFactory.class)
						.withMembers(MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
	}

}
