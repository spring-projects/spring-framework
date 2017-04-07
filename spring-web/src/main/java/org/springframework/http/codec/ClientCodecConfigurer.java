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
 * Helps to configure a list of client-side HTTP message readers and writers
 * with support for built-in defaults and options to register additional custom
 * readers and writers via {@link #customCodecs()}.
 *
 * <p>The built-in defaults include basic data types such as various byte
 * representations, resources, strings, forms, but also others like JAXB2 and
 * Jackson 2 based on classpath detection. There are options to
 * {@link #defaultCodecs() override} some of the defaults or to have them
 * {@link #registerDefaults(boolean) turned off} completely.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ClientCodecConfigurer extends AbstractCodecConfigurer {


	public ClientCodecConfigurer() {
		super(new ClientDefaultCodecConfigurer());
	}


	@Override
	public ClientDefaultCodecConfigurer defaultCodecs() {
		return (ClientDefaultCodecConfigurer) super.defaultCodecs();
	}


	@Override
	protected void addDefaultTypedWriter(List<HttpMessageWriter<?>> result) {
		super.addDefaultTypedWriter(result);
		defaultCodecs().addWriterTo(result, FormHttpMessageWriter::new);
	}

	@Override
	protected void addDefaultObjectReaders(List<HttpMessageReader<?>> result) {
		super.addDefaultObjectReaders(result);
		defaultCodecs().addServerSentEventReaderTo(result);
	}


	/**
	 * Extension of {@code DefaultCodecConfigurer} with extra client options.
	 */
	public static class ClientDefaultCodecConfigurer extends DefaultCodecConfigurer {

		/**
		 * Configure the {@code Decoder} to use for Server-Sent Events.
		 * <p>By default the {@link #jackson2Decoder} override is used for SSE.
		 * @param decoder the decoder to use
		 */
		public void serverSentEventDecoder(Decoder<?> decoder) {
			HttpMessageReader<?> reader = new ServerSentEventHttpMessageReader(decoder);
			getReaders().put(ServerSentEventHttpMessageReader.class, reader);
		}


		// Internal methods for building a list of default readers or writers...

		@Override
		protected void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(false)));
		}

		@Override
		protected void addStringReaderTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(false)));
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
	}

}
