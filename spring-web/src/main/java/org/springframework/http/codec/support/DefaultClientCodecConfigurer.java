/*
 * Copyright 2002-2017 the original author or authors.
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
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultClientCodecConfigurer extends AbstractCodecConfigurer implements ClientCodecConfigurer {

	public DefaultClientCodecConfigurer() {
		super(new ClientDefaultCodecsImpl());
	}

	@Override
	public ClientDefaultCodecs defaultCodecs() {
		return (ClientDefaultCodecs) super.defaultCodecs();
	}


	private static class ClientDefaultCodecsImpl extends AbstractDefaultCodecs implements ClientDefaultCodecs {

		@Nullable
		private DefaultMultipartCodecs multipartCodecs;

		@Nullable
		private Decoder<?> sseDecoder;

		@Override
		public MultipartCodecs multipartCodecs() {
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
		boolean splitTextOnNewLine() {
			return false;
		}

		@Override
		List<HttpMessageReader<?>> getObjectReaders() {
			if (!shouldRegisterDefaults()) {
				return Collections.emptyList();
			}
			List<HttpMessageReader<?>> result = super.getObjectReaders();
			result.add(new ServerSentEventHttpMessageReader(getSseDecoder()));
			return result;
		}

		@Nullable
		private Decoder<?> getSseDecoder() {
			if (this.sseDecoder != null) {
				return this.sseDecoder;
			}
			return (jackson2Present ? getJackson2JsonDecoder() : null);
		}

		@Override
		List<HttpMessageWriter<?>> getTypedWriters() {
			if (!shouldRegisterDefaults()) {
				return Collections.emptyList();
			}
			List<HttpMessageWriter<?>> result = super.getTypedWriters();
			result.add(new MultipartHttpMessageWriter(getPartWriters(), new FormHttpMessageWriter()));
			return result;
		}

		private List<HttpMessageWriter<?>> getPartWriters() {
			List<HttpMessageWriter<?>> partWriters;
			if (this.multipartCodecs != null) {
				partWriters = this.multipartCodecs.getWriters();
			}
			else {
				DefaultCustomCodecs customCodecs = getCustomCodecs();
				partWriters = new ArrayList<>();
				partWriters.addAll(super.getTypedWriters());
				if (customCodecs != null) {
					partWriters.addAll(customCodecs.getTypedWriters());
				}
				partWriters.addAll(super.getObjectWriters());
				if (customCodecs != null) {
					partWriters.addAll(customCodecs.getObjectWriters());
				}
				partWriters.addAll(super.getCatchAllWriters());
			}
			return partWriters;
		}
	}


	private static class DefaultMultipartCodecs implements MultipartCodecs {

		private final List<HttpMessageWriter<?>> writers = new ArrayList<>();

		@Override
		public MultipartCodecs encoder(Encoder<?> encoder) {
			writer(new EncoderHttpMessageWriter<>(encoder));
			return this;
		}

		@Override
		public MultipartCodecs writer(HttpMessageWriter<?> writer) {
			this.writers.add(writer);
			return this;
		}

		List<HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}
	}

}
