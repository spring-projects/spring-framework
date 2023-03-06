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

package org.springframework.http.codec.support;

import java.util.List;

import org.springframework.core.codec.Decoder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

	@Nullable
	private Decoder<?> sseDecoder;


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
				jackson2Present ? getJackson2JsonDecoder() :
				kotlinSerializationJsonPresent ? getKotlinSerializationJsonDecoder() :
				null);

		addCodec(objectReaders, new ServerSentEventHttpMessageReader(decoder));
	}

}
