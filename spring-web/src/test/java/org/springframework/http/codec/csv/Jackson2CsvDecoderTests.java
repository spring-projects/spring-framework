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

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link Jackson2CsvDecoder}.
 */
class Jackson2CsvDecoderTests {

	/**
	 * Type for rows: {@code Map<String, String>}.
	 */
	private static final ResolvableType CSV_RECORD_TYPE =
			ResolvableType.forClassWithGenerics(Map.class, String.class, String.class);

	/**
	 * MIME type for CSV encoded as UTF-8.
	 */
	private static final MimeType MIME_TYPE_CSV_UTF_8 =
			MimeType.valueOf("text/csv;charset=UTF-8");

	/**
	 * Test for {@link Jackson2CsvDecoder#decode(Publisher, ResolvableType, MimeType, Map)}.
	 */
	@Test
	void decode() {
		var schema = CsvSchema.builder()
				.setColumnSeparator(';')
				.setLineSeparator('\n')
				.addColumn("header1")
				.addColumn("header2");
		var decoder = decoder(schema);
		decoder.setMaxInMemorySize(16);

		var buffers = encodeUtf8("""
				value1;value2
				value3;value4
				""");

		var rows = decoder.decode(buffers, CSV_RECORD_TYPE, MIME_TYPE_CSV_UTF_8, Map.of());

		assertThat(rows.toIterable()).containsExactly(
				Map.of("header1", "value1", "header2", "value2"),
				Map.of("header1", "value3", "header2", "value4"));
	}

	/**
	 * Create a decoder for the schema.
	 */
	private Jackson2CsvDecoder<Map<String, String>> decoder(CsvSchema.Builder schema) {
		var csvMapper = CsvMapper.builder().build();
		return new Jackson2CsvDecoder<>(csvMapper, schema.build());
	}

	/**
	 * Encode the CSV as UTF-8 and use {@code \n} as line separators.
	 */
	private Flux<DataBuffer> encodeUtf8(String csv) {
		var encoded = csv.replaceAll("\\R", "\n").getBytes(UTF_8);
		return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(encoded));
	}

}