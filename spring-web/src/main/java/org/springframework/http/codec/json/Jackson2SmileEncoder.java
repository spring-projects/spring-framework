/*
 * Copyright 2002-2019 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of Smile objects using Jackson 2.9.
 * For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see Jackson2SmileDecoder
 */
public class Jackson2SmileEncoder extends AbstractJackson2Encoder {

	private static final MimeType[] DEFAULT_SMILE_MIME_TYPES = new MimeType[] {
			new MimeType("application", "x-jackson-smile"),
			new MimeType("application", "*+x-jackson-smile")};


	public Jackson2SmileEncoder() {
		this(Jackson2ObjectMapperBuilder.smile().build(), DEFAULT_SMILE_MIME_TYPES);
	}

	public Jackson2SmileEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		Assert.isAssignable(SmileFactory.class, mapper.getFactory().getClass());
		setStreamingMediaTypes(Collections.singletonList(new MediaType("application", "stream+x-jackson-smile")));
	}

}
