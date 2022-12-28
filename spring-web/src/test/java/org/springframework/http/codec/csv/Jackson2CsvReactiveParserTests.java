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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class Jackson2CsvReactiveParserTests {

	/**
	 * Type for rows: {@code Map<String, String>}.
	 */
	private static final TypeReference<Map<String, String>> CSV_RECORD_TYPE = new TypeReference<>() {
	};

	@Test
	void parse_withHeader() {
		var parser = parser(CsvSchema.builder()
						.setColumnSeparator(';')
						.setLineSeparator('\n')
						// Read header from CSV.
						.setUseHeader(true),
				3);

		var rows = rows("header1;header2", "value1;value2", "value3;value4")
				.flatMap(parser::parse)
				.concatWith(Flux.defer(parser::parseRemaining))
				.toIterable();

		assertThat(rows).containsOnly(
				Map.of("header1", "value1", "header2", "value2"),
				Map.of("header1", "value3", "header2", "value4"));
	}

	@Test
	void parse_withoutHeader() {
		var parser = parser(CsvSchema.builder()
						.setColumnSeparator(';')
						.setLineSeparator('\n')
						.addColumn("header1")
						.addColumn("header2"),
				2);

		var rows = rows("value1;value2", "value3;value4")
				.flatMap(parser::parse)
				.concatWith(Flux.defer(parser::parseRemaining))
				.toIterable();

		assertThat(rows).containsOnly(
				Map.of("header1", "value1", "header2", "value2"),
				Map.of("header1", "value3", "header2", "value4"));
	}

	/**
	 * Create a parser for the schema.
	 */
	private Jackson2CsvReactiveParser<Map<String, String>> parser(CsvSchema.Builder schema, int lookahead) {
		var csvMapper = CsvMapper.builder().build();
		var objectReader = csvMapper
				.readerFor(CSV_RECORD_TYPE)
				.with(schema.build());
		return new Jackson2CsvReactiveParser<>(objectReader, lookahead);
	}

	/**
	 * Create rows.
	 */
	private Flux<String> rows(String... rows) {
		return Flux.just(rows).map(row -> row + "\n");
	}

}