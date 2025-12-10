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

import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.smile.SmileMapper;

import org.springframework.http.codec.AbstractJacksonDecoder;
import org.springframework.util.MimeType;

/**
 * Decode a byte stream into Smile and convert to Objects with Jackson 3.x,
 * leveraging non-blocking parsing.
 *
 * @author Sebastien Deleuze
 * @since 7.0
 * @see JacksonSmileEncoder
 */
public class JacksonSmileDecoder extends AbstractJacksonDecoder<SmileMapper> {

	private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[] {
					new MimeType("application", "x-jackson-smile"),
					new MimeType("application", "*+x-jackson-smile")};


	/**
	 * Construct a new instance with a {@link SmileMapper} customized with the
	 * {@link tools.jackson.databind.JacksonModule}s found by
	 * {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileDecoder() {
		super(SmileMapper.builder(), DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileDecoder(SmileMapper.Builder builder) {
		this(builder, DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper}.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileDecoder(SmileMapper mapper) {
		this(mapper, DEFAULT_SMILE_MIME_TYPES);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper.Builder}
	 * customized with the {@link tools.jackson.databind.JacksonModule}s
	 * found by {@link MapperBuilder#findModules(ClassLoader)}, and
	 * {@link MimeType}s.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileDecoder(SmileMapper.Builder builder, MimeType... mimeTypes) {
		super(builder, mimeTypes);
	}

	/**
	 * Construct a new instance with the provided {@link SmileMapper} and {@link MimeType}s.
	 * @see SmileMapper#builder()
	 */
	public JacksonSmileDecoder(SmileMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}

}
