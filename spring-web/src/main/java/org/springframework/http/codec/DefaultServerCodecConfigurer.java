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

import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;

/**
 * Default implementation of {@link ServerCodecConfigurer}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultServerCodecConfigurer extends DefaultCodecConfigurer implements ServerCodecConfigurer {

	public DefaultServerCodecConfigurer() {
		super(new DefaultServerDefaultCodecsConfigurer());
	}

	@Override
	public ServerDefaultCodecsConfigurer defaultCodecs() {
		return (ServerDefaultCodecsConfigurer) super.defaultCodecs();
	}


	/**
	 * Default implementation of {@link ServerCodecConfigurer.ServerDefaultCodecsConfigurer}.
	 */
	private static class DefaultServerDefaultCodecsConfigurer
			extends AbstractDefaultCodecsConfigurer
			implements ServerCodecConfigurer.ServerDefaultCodecsConfigurer {

		@Override
		public void serverSentEventEncoder(Encoder<?> encoder) {
			HttpMessageWriter<?> writer = new ServerSentEventHttpMessageWriter(encoder);
			getWriters().put(ServerSentEventHttpMessageWriter.class, writer);
		}

		@Override
		public void addTypedReadersTo(List<HttpMessageReader<?>> result) {
			super.addTypedReadersTo(result);
			addReaderTo(result, FormHttpMessageReader::new);
		}

		@Override
		protected void addObjectWritersTo(List<HttpMessageWriter<?>> result) {
			super.addObjectWritersTo(result);
			addServerSentEventWriterTo(result);
		}


		private void addServerSentEventWriterTo(List<HttpMessageWriter<?>> result) {
			addWriterTo(result, () -> findWriter(ServerSentEventHttpMessageWriter.class, () -> {
				Encoder<?> encoder = null;
				if (jackson2Present) {
					encoder = findEncoderWriter(
							Jackson2JsonEncoder.class, Jackson2JsonEncoder::new).getEncoder();
				}
				return new ServerSentEventHttpMessageWriter(encoder);
			}));
		}

		@Override
		protected void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result,
					() -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(true)));
		}

		@Override
		protected void addStringReaderTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result,
					() -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
		}

	}

}
