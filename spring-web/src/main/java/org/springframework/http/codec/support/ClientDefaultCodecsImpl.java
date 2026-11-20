/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.support;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.codec.Decoder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;

/**
 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

	private @Nullable Decoder<?> sseDecoder;


	ClientDefaultCodecsImpl() {
	}

	ClientDefaultCodecsImpl(ClientDefaultCodecsImpl other) {
		super(other);
		this.sseDecoder = other.sseDecoder;
	}

	@Override
	public void serverSentEventDecoder(Decoder<?> decoder) {
		this.sseDecoder = decoder;
		initObjectReaders();
	}

	@Override
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {

		Decoder<?> decoder = (this.sseDecoder != null ? this.sseDecoder :
				(JACKSON_PRESENT || JACKSON_2_PRESENT) ? getJacksonJsonDecoder() :
				GSON_PRESENT ? getGsonDecoder() :
				KOTLIN_SERIALIZATION_JSON_PRESENT ? getKotlinSerializationJsonDecoder() :
				null);

		addCodec(objectReaders, new ServerSentEventHttpMessageReader(decoder));
	}

}
