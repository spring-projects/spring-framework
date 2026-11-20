/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.http.codec.support.DefaultClientCodecConfigurer;
import org.springframework.http.codec.support.DefaultServerCodecConfigurer;

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
