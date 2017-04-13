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

import java.util.List;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.json.Jackson2JsonDecoder;

/**
 * Default implementation of {@link ClientCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultClientCodecConfigurer extends DefaultCodecConfigurer implements ClientCodecConfigurer {

	public DefaultClientCodecConfigurer() {
		super(new DefaultClientDefaultCodecsConfigurer());
	}

	@Override
	public ClientDefaultCodecsConfigurer defaultCodecs() {
		return (ClientDefaultCodecsConfigurer) super.defaultCodecs();
	}


	/**
	 * Default implementation of {@link ClientCodecConfigurer.ClientDefaultCodecsConfigurer}.
	 */
	private static class DefaultClientDefaultCodecsConfigurer
			extends AbstractDefaultCodecsConfigurer
			implements ClientCodecConfigurer.ClientDefaultCodecsConfigurer {

		@Override
		public void serverSentEventDecoder(Decoder<?> decoder) {
			HttpMessageReader<?> reader = new ServerSentEventHttpMessageReader(decoder);
			getReaders().put(ServerSentEventHttpMessageReader.class, reader);
		}

		@Override
		protected void addTypedWritersTo(List<HttpMessageWriter<?>> result) {
			super.addTypedWritersTo(result);
			addWriterTo(result, FormHttpMessageWriter::new);
		}

		@Override
		protected void addObjectReadersTo(List<HttpMessageReader<?>> result) {
			super.addObjectReadersTo(result);
			addServerSentEventReaderTo(result);
		}

		private void addServerSentEventReaderTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> findReader(ServerSentEventHttpMessageReader.class, () -> {
				Decoder<?> decoder = null;
				if (jackson2Present) {
					decoder = findDecoderReader(
							Jackson2JsonDecoder.class, Jackson2JsonDecoder::new).getDecoder();
				}
				return new ServerSentEventHttpMessageReader(decoder);
			}));
		}

		@Override
		protected void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result,
					() -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(false)));
		}

		@Override
		protected void addStringReaderTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result,
					() -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(false)));
		}

	}

}
