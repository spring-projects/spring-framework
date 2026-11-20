/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.codec.support;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.ServerSentEventHttpMessageWriter;

/**
 * Default implementation of {@link ServerCodecConfigurer.ServerDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 */
class ServerDefaultCodecsImpl extends BaseDefaultCodecs implements ServerCodecConfigurer.ServerDefaultCodecs {

	private @Nullable Encoder<?> sseEncoder;


	ServerDefaultCodecsImpl() {
	}

	ServerDefaultCodecsImpl(ServerDefaultCodecsImpl other) {
		super(other);
		this.sseEncoder = other.sseEncoder;
	}


	@Override
	public void serverSentEventEncoder(Encoder<?> encoder) {
		this.sseEncoder = encoder;
		initObjectWriters();
	}

	@Override
	protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
		objectWriters.add(new ServerSentEventHttpMessageWriter(getSseEncoder()));
	}

	private @Nullable Encoder<?> getSseEncoder() {
		return this.sseEncoder != null ? this.sseEncoder :
				(JACKSON_PRESENT || JACKSON_2_PRESENT) ? getJacksonJsonEncoder() :
				GSON_PRESENT ? getGsonEncoder() :
				KOTLIN_SERIALIZATION_JSON_PRESENT ? getKotlinSerializationJsonEncoder() :
				null;
	}

}
