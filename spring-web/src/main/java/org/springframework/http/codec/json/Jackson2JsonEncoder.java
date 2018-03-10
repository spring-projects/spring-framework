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

package org.springframework.http.codec.json;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.PrettyPrinter;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * Encode from an {@code Object} stream to a byte stream of JSON objects using Jackson 2.9.
 * For non-streaming use cases, {@link Flux} elements are collected into a {@link List}
 * before serialization for performance reason.
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 * @see Jackson2JsonDecoder
 */
public class Jackson2JsonEncoder extends AbstractJackson2Encoder {

	@Nullable
	private final PrettyPrinter ssePrettyPrinter;
	

	public Jackson2JsonEncoder() {
		this(Jackson2ObjectMapperBuilder.json().build());
	}

	public Jackson2JsonEncoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
		setStreamingMediaTypes(Collections.singletonList(MediaType.APPLICATION_STREAM_JSON));
		this.ssePrettyPrinter = initSsePrettyPrinter();
	}

	private static PrettyPrinter initSsePrettyPrinter() {
		DefaultPrettyPrinter printer = new DefaultPrettyPrinter();
		printer.indentObjectsWith(new DefaultIndenter("  ", "\ndata:"));
		return printer;
	}


	@Override
	protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
			ResolvableType elementType, @Nullable Map<String, Object> hints) {
		
		return (this.ssePrettyPrinter != null &&
				MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mimeType) &&
				writer.getConfig().isEnabled(SerializationFeature.INDENT_OUTPUT) ?
				writer.with(this.ssePrettyPrinter) : writer);
	}

}
