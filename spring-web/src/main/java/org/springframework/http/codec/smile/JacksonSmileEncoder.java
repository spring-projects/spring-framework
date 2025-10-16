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

package org.springframework.http.codec.smile;

import java.util.Collections;
import java.util.List;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.smile.SmileMapper;

import org.springframework.http.MediaType;
import org.springframework.http.codec.AbstractJacksonEncoder;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of Smile objects using Jackson 3.x.
 *
 * <p>For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reasons.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonSmileDecoder
 */
public class JacksonSmileEncoder extends AbstractJacksonEncoder<SmileMapper> {

	private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[] {
			new MimeType("application", "x-jackson-smile"),
			new MimeType("application", "*+x-jackson-smile")};

	private static final MediaType DEFAULT_SMILE_STREAMING_MEDIA_TYPE =
			new MediaType("application", "stream+x-jackson-smile");

	private static final byte[] STREAM_SEPARATOR = new byte[0];


	/**
	 * Construct a new instance with a {@link SmileMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 */
	public JacksonSmileEncoder() {
		this(SmileMapper.builder(), DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileEncoder(SmileMapper.Builder builder) {
		this(builder, DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileEncoder(SmileMapper mapper) {
		this(mapper, DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}, and
	 * {@link MimeType}s.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileEncoder(SmileMapper.Builder builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
		setStreamingMediaTypes(Collections.singletonList(DEFAULT_SMILE_STREAMING_MEDIA_TYPE));
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper} and {@link MimeType}s.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileEncoder(SmileMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		setStreamingMediaTypes(Collections.singletonList(DEFAULT_SMILE_STREAMING_MEDIA_TYPE));
	}


	/**
	 * Return the separator to use for the given mime type.
	 * <p>By default, this method returns a single byte 0 if the given
	 * mime type is one of the configured {@link #setStreamingMediaTypes(List)
	 * streaming} mime types.
	 */
	@Override
	protected byte @Nullable [] getStreamingMediaTypeSeparator(@Nullable MimeType mimeType) {
		for (MediaType streamingMediaType : getStreamingMediaTypes()) {
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				return STREAM_SEPARATOR;
			}
		}
		return null;
	}

}
