/*
 * Copyright 2002-present the original author or authors.
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

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CharBufferDecoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonDecoder;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * Decode a byte stream into JSON and convert to Object's with
 * <a href="https://github.com/FasterXML/jackson">Jackson 3.x</a>
 * leveraging non-blocking parsing.
 *
 * <p>The default constructor loads {@link tools.jackson.databind.JacksonModule}s
 * found by {@link MapperBuilder#findModules(ClassLoader)}.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonJsonEncoder
 */
public class JacksonJsonDecoder extends AbstractJacksonDecoder<JsonMapper> {

	private static final CharBufferDecoder CHAR_BUFFER_DECODER = CharBufferDecoder.textPlainOnly(Arrays.asList(",", "\n"), false);

	private static final ResolvableType CHAR_BUFFER_TYPE = ResolvableType.forClass(CharBuffer.class);

	private static final MimeType[] DEFAULT_JSON_MIME_TYPES = new MimeType[] {
			MediaType.APPLICATION_JSON,
			new MediaType("application", "*+json"),
			MediaType.APPLICATION_NDJSON
	};


	/**
	 * Construct a new instance with a {@link JsonMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonJsonDecoder() {
		super(JsonMapper.builder(), DEFAULT_JSON_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper}.
	 * @see JsonMapper#builder()
	 * @see MapperBuilder#findModules(ClassLoader)
	 */
	public JacksonJsonDecoder(JsonMapper mapper) {
		this(mapper, DEFAULT_JSON_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link JsonMapper} and {@link MimeType}s.
	 * @see JsonMapper#builder()
	 * @see MapperBuilder#findModules(ClassLoader)
	 */
	public JacksonJsonDecoder(JsonMapper mapper, MimeType... mimeTypes) {
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

		// Re-encode as UTF-8.
		MimeType textMimeType = new MimeType(MimeTypeUtils.TEXT_PLAIN, charset);
		Flux<CharBuffer> decoded = CHAR_BUFFER_DECODER.decode(input, CHAR_BUFFER_TYPE, textMimeType, null);
		return decoded.map(charBuffer -> DefaultDataBufferFactory.sharedInstance.wrap(StandardCharsets.UTF_8.encode(charBuffer)));
	}

}
