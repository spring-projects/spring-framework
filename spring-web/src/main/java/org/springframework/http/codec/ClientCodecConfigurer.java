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

package org.springframework.http.codec;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;

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
public interface ClientCodecConfigurer extends CodecConfigurer {

	@Override
	ClientDefaultCodecs defaultCodecs();


	/**
	 * Create a new instance of the {@code ClientCodecConfigurer}.
	 */
	static ClientCodecConfigurer create() {
		return CodecConfigurerFactory.create(ClientCodecConfigurer.class);
	}


	/**
	 * Extension of {@link CodecConfigurer.DefaultCodecs} with extra client options.
	 */
	interface ClientDefaultCodecs extends DefaultCodecs {

		/**
		 * Configure encoders or writers for use with
		 * {@link org.springframework.http.codec.multipart.MultipartHttpMessageWriter
		 * MultipartHttpMessageWriter}.
		 */
		MultipartCodecs multipartCodecs();

		/**
		 * Configure the {@code Decoder} to use for Server-Sent Events.
		 * <p>By default if this is not set, and Jackson is available, the
		 * {@link #jackson2JsonDecoder} override is used instead. Use this property
		 * if you want to further customize the SSE decoder.
		 * @param decoder the decoder to use
		 */
		void serverSentEventDecoder(Decoder<?> decoder);
	}


	/**
	 * Registry and container for multipart HTTP message writers.
	 */
	interface MultipartCodecs {

		/**
		 * Add a Part {@code Encoder}, internally wrapped with
		 * {@link EncoderHttpMessageWriter}.
		 * @param encoder the encoder to add
		 */
		MultipartCodecs encoder(Encoder<?> encoder);

		/**
		 * Add a Part {@link HttpMessageWriter}. For writers of type
		 * {@link EncoderHttpMessageWriter} consider using the shortcut
		 * {@link #encoder(Encoder)} instead.
		 * @param writer the writer to add
		 */
		MultipartCodecs writer(HttpMessageWriter<?> writer);
	}

}
