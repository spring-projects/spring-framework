/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.csv;

import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.AbstractEncoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Encoder for CSV files.
 */
public final class Jackson2CsvEncoder<T> extends AbstractEncoder<T> {

	/**
	 * CSV mapper.
	 */
	private final CsvMapper mapper;

	/**
	 * CSV schema.
	 */
	private final CsvSchema schema;

	/**
	 * Default charset. Defaults to UTF-8.
	 */
	private Charset defaultCharset = UTF_8;

	/**
	 * Constructor.
	 */
	public Jackson2CsvEncoder(CsvMapper mapper, CsvSchema schema) {
		super(MimeType.valueOf("text/csv"));
		Assert.notNull(mapper, "mapper must not be null");
		Assert.notNull(schema, "schema must not be null");
		this.mapper = mapper;
		this.schema = schema;
	}

	/**
	 * Default charset. Defaults to UTF-8.
	 */
	public Charset getDefaultCharset() {
		return defaultCharset;
	}

	/**
	 * Default charset. Defaults to UTF-8.
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "defaultCharset must not be null");
		this.defaultCharset = defaultCharset;
	}

	@Override
	public Flux<DataBuffer> encode(
			Publisher<? extends T> inputStream, DataBufferFactory bufferFactory, ResolvableType elementType,
			@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {
		Assert.notNull(inputStream, "inputStream must not be null");
		Assert.notNull(bufferFactory, "bufferFactory must not be null");
		Assert.notNull(elementType, "elementType must not be null");

		var objectWriter = mapper.writerFor(mapper.constructType(elementType.getType())).with(schema);
		var charset = mimeType != null && mimeType.getCharset() != null ? mimeType.getCharset() : defaultCharset;

		return Flux.from(inputStream)
				.flatMap(row -> writeCsv(row, objectWriter))
				.map(charset::encode)
				.map(bufferFactory::wrap);
	}

	/**
	 * Write a row.
	 */
	private Mono<String> writeCsv(T row, ObjectWriter objectWriter) {
		try {
			return Mono.just(objectWriter.writeValueAsString(row));
		} catch (Exception e) {
			return Mono.error(e);
		}
	}

}
