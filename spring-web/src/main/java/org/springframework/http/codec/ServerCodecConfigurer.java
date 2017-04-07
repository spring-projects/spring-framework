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
 * Helps to configure a list of server-side HTTP message readers and writers
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
public class ServerCodecConfigurer extends AbstractCodecConfigurer {


	public ServerCodecConfigurer() {
		super(new ServerDefaultCodecConfigurer());
	}


	@Override
	public ServerDefaultCodecConfigurer defaultCodecs() {
		return (ServerDefaultCodecConfigurer) super.defaultCodecs();
	}


	@Override
	protected void addDefaultTypedReaders(List<HttpMessageReader<?>> result) {
		super.addDefaultTypedReaders(result);
		defaultCodecs().addReaderTo(result, FormHttpMessageReader::new);
	}


	@Override
	protected void addDefaultObjectWriters(List<HttpMessageWriter<?>> result) {
		super.addDefaultObjectWriters(result);
		defaultCodecs().addServerSentEventWriterTo(result);
	}


	/**
	 * Extension of {@code DefaultCodecConfigurer} with extra server options.
	 */
	public static class ServerDefaultCodecConfigurer extends DefaultCodecConfigurer {

		/**
		 * Configure the {@code Encoder} to use for Server-Sent Events.
		 * <p>By default the {@link #jackson2Encoder} override is used for SSE.
		 * @param encoder the encoder to use
		 */
		public void serverSentEventEncoder(Encoder<?> encoder) {
			HttpMessageWriter<?> writer = new ServerSentEventHttpMessageWriter(encoder);
			getWriters().put(ServerSentEventHttpMessageWriter.class, writer);
		}


		// Internal methods for building a list of default readers or writers...

		@Override
		protected void addStringReaderTextOnlyTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly(true)));
		}

		@Override
		protected void addStringReaderTo(List<HttpMessageReader<?>> result) {
			addReaderTo(result, () -> new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes(true)));
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
	}

}
