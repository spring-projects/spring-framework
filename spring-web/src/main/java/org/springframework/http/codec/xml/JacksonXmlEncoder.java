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

package org.springframework.http.codec.xml;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.xml.XmlMapper;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonEncoder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} to bytes of XML objects using Jackson 3.x.
 *
 * <p>Stream encoding is currently not supported.
 *
 * @author Sebastien Deleuze
 * @since 7.0.3
 * @see JacksonXmlDecoder
 */
public class JacksonXmlEncoder extends AbstractJacksonEncoder<XmlMapper> {

	private static final MediaType[] DEFAULT_XML_MIME_TYPES = new MediaType[] {
			new MediaType("application", "xml", StandardCharsets.UTF_8),
			new MediaType("text", "xml", StandardCharsets.UTF_8),
			new MediaType("application", "*+xml", StandardCharsets.UTF_8)
	};


	/**
	 * Construct a new instance with a {@link XmlMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonXmlEncoder() {
		super(XmlMapper.builder(), DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper.Builder builder) {
		super(builder, DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper}.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper mapper) {
		super(mapper, DEFAULT_XML_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}, and
	 * {@link MimeType}s.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper.Builder builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link XmlMapper} and {@link MimeType}s.
	 * @see XmlMapper#builder()
	 */
	public JacksonXmlEncoder(XmlMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		throw new UnsupportedOperationException("Stream encoding is currently not supported");
	}

}
