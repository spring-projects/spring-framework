/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.FormHttpMessageWriter;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEventHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.lang.Nullable;

/**
 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecs}.
 *
 * @author Rossen Stoyanchev
 */
class ClientDefaultCodecsImpl extends BaseDefaultCodecs implements ClientCodecConfigurer.ClientDefaultCodecs {

	@Nullable
	private DefaultMultipartCodecs multipartCodecs;

	@Nullable
	private Decoder<?> sseDecoder;

	@Nullable
	private Supplier<List<HttpMessageWriter<?>>> partWritersSupplier;


	/**
	 * Set a supplier for part writers to use when
	 * {@link #multipartCodecs()} are not explicitly configured.
	 * That's the same set of writers as for general except for the multipart
	 * writer itself.
	 */
	void setPartWritersSupplier(Supplier<List<HttpMessageWriter<?>>> supplier) {
		this.partWritersSupplier = supplier;
	}


	@Override
	public ClientCodecConfigurer.MultipartCodecs multipartCodecs() {
		if (this.multipartCodecs == null) {
			this.multipartCodecs = new DefaultMultipartCodecs();
		}
		return this.multipartCodecs;
	}

	@Override
	public void serverSentEventDecoder(Decoder<?> decoder) {
		this.sseDecoder = decoder;
	}


	@Override
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {
		objectReaders.add(new ServerSentEventHttpMessageReader(getSseDecoder()));
	}

	@Nullable
	private Decoder<?> getSseDecoder() {
		return (this.sseDecoder != null ? this.sseDecoder : jackson2Present ? getJackson2JsonDecoder() : null);
	}

	@Override
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
		typedWriters.add(new MultipartHttpMessageWriter(getPartWriters(), new FormHttpMessageWriter()));
	}

	private List<HttpMessageWriter<?>> getPartWriters() {
		if (this.multipartCodecs != null) {
			return this.multipartCodecs.getWriters();
		}
		else if (this.partWritersSupplier != null) {
			return this.partWritersSupplier.get();
		}
		else {
			return Collections.emptyList();
		}
	}


	/**
	 * Default implementation of {@link ClientCodecConfigurer.MultipartCodecs}.
	 */
	private static class DefaultMultipartCodecs implements ClientCodecConfigurer.MultipartCodecs {

		private final List<HttpMessageWriter<?>> writers = new ArrayList<>();

		@Override
		public ClientCodecConfigurer.MultipartCodecs encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
			return this;
		}

		@Override
		public ClientCodecConfigurer.MultipartCodecs writer(HttpMessageWriter<?> writer) {
			this.writers.add(writer);
			return this;
		}

		List<HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}
	}

}
