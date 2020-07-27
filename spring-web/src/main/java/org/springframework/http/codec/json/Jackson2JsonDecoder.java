/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.http.codec.json;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decode a byte stream into JSON and convert to Object's with Jackson 2.9,
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see Jackson2JsonEncoder
 */
public class Jackson2JsonDecoder extends AbstractJackson2Decoder {

	private static final StringDecoder STRING_DECODER = StringDecoder.textPlainOnly(Arrays.asList(",", "\n"), false);

	private static final ResolvableType STRING_TYPE = ResolvableType.forClass(String.class);


	public Jackson2JsonDecoder() {
		super(Jackson2ObjectMapperBuilder.json().build());
	}

	public Jackson2JsonDecoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}

	@Override
	protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Flux<DataBuffer> flux = Flux.from(input);
		if (mimeType == null) {
			return flux;
		}

		// Jackson asynchronous parser only supports UTF-8
		Charset charset = mimeType.getCharset();
		if (charset == null || StandardCharsets.UTF_8.equals(charset) || StandardCharsets.US_ASCII.equals(charset)) {
			return flux;
		}

		// Potentially, the memory consumption of this conversion could be improved by using CharBuffers instead
		// of allocating Strings, but that would require refactoring the buffer tokenization code from StringDecoder

		MimeType textMimeType = new MimeType(MimeTypeUtils.TEXT_PLAIN, charset);
		Flux<String> decoded = STRING_DECODER.decode(input, STRING_TYPE, textMimeType, null);
		return decoded.map(s -> DefaultDataBufferFactory.sharedInstance.wrap(s.getBytes(StandardCharsets.UTF_8)));
	}

}
