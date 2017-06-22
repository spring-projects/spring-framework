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

package org.springframework.http.codec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;

/**
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultClientCodecConfigurer extends AbstractCodecConfigurer implements ClientCodecConfigurer {


	public DefaultClientCodecConfigurer() {
		super(new ClientDefaultCodecsImpl());
	}


	@Override
	public ClientDefaultCodecs defaultCodecs() {
		return (ClientDefaultCodecs) super.defaultCodecs();
	}


	private static class ClientDefaultCodecsImpl extends AbstractDefaultCodecs
			implements ClientDefaultCodecs {

		private DefaultMultipartCodecs multipartCodecs;

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
		protected boolean splitTextOnNewLine() {
			return false;
		}

		@Override
		public List<HttpMessageReader<?>> getObjectReaders() {
			if (!shouldRegisterDefaults()) {
				return Collections.emptyList();
			}
			List<HttpMessageReader<?>> result = super.getObjectReaders();
			result.add(new ServerSentEventHttpMessageReader(getSseDecoder()));
			return result;
		}

		private Decoder<?> getSseDecoder() {
			if (this.sseDecoder != null) {
				return this.sseDecoder;
			}
			return (jackson2Present ? jackson2Decoder() : null);
		}

		@Override
		public List<HttpMessageWriter<?>> getTypedWriters() {
			if (!this.shouldRegisterDefaults()) {
				return Collections.emptyList();
			}
			List<HttpMessageWriter<?>> result = super.getTypedWriters();
			result.add(new FormHttpMessageWriter());
			result.add(getMultipartHttpMessageWriter());
			return result;
		}

		private MultipartHttpMessageWriter getMultipartHttpMessageWriter() {
			List<HttpMessageWriter<?>> partWriters;
			if (this.multipartCodecs != null) {
				partWriters = this.multipartCodecs.getWriters();
			}
			else {
				partWriters = new ArrayList<>();
				partWriters.addAll(super.getTypedWriters());
				partWriters.addAll(getCustomCodecs().getTypedWriters());
				partWriters.addAll(super.getObjectWriters());
				partWriters.addAll(getCustomCodecs().getObjectWriters());
				partWriters.addAll(super.getCatchAllWriters());
			}
			return new MultipartHttpMessageWriter(partWriters);
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

		public List<HttpMessageWriter<?>> getWriters() {
			return this.writers;
		}
	}

}
