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

import java.util.Map;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.MimeType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.io.buffer.DataBufferUtils.join;

class Jackson2CsvEncoderTests {

	/**
	 * Type for rows: {@code Map<String, String>}.
	 */
	private static final ResolvableType CSV_RECORD_TYPE =
			ResolvableType.forClassWithGenerics(Map.class, String.class, String.class);

	/**
	 * MIME type for CSV encoded as UTF-8.
	 */
	private static final MimeType MIME_TYPE_CSV_UTF_8 = MimeType.valueOf("text/csv;charset=UTF-8");

	@Test
	void encode() {
		var schema = CsvSchema.builder()
				.disableQuoteChar()
				.setColumnSeparator(';')
				.setLineSeparator('\n')
				.setUseHeader(true)
				.addColumn("header1")
				.addColumn("header2");
		var encoder = encoder(schema);

		var rows = Flux.just(Map.of("header1", "value1", "header2", "value2"));
		var bufferFactory = DefaultDataBufferFactory.sharedInstance;

		var csv = encoder.encode(rows, bufferFactory, CSV_RECORD_TYPE, MIME_TYPE_CSV_UTF_8, Map.of());

		assertThat(join(csv).block().asInputStream()).hasBinaryContent(encodeUtf8("""
				header1;header2
				value1;value2
				"""));
	}

	/**
	 * Create an encoder for the schema.
	 */
	private Jackson2CsvEncoder<Map<String, String>> encoder(CsvSchema.Builder schema) {
		var csvMapper = CsvMapper.builder().build();
		return new Jackson2CsvEncoder<>(csvMapper, schema.build());
	}

	/**
	 * Encode the CSV as UTF-8 and use {@code \n} as line separators.
	 */
	private byte[] encodeUtf8(String csv) {
		return csv
				.replaceAll("\\R", "\n")
				.getBytes(UTF_8);
	}

}
