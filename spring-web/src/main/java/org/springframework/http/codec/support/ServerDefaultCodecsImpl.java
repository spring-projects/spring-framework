/*
 * Copyright 2002-2025 the original author or authors.
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
				jacksonPresent ? getJacksonJsonEncoder() :
				jackson2Present ? getJackson2JsonEncoder() :
				kotlinSerializationJsonPresent ? getKotlinSerializationJsonEncoder() :
				null;
	}

}
