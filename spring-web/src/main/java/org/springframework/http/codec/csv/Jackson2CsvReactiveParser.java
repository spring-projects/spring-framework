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

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Reactive CSV parser. NOT thread-safe.
 */
final class Jackson2CsvReactiveParser<T> {

	/**
	 * CSV object reader.
	 */
	private final ObjectReader objectReader;

	/**
	 * Lookahead. Number of CSV rows to accumulate in the reader before start parsing them.
	 */
	private final int lookahead;

	/**
	 * Reader with the next CSV rows.
	 */
	private final MultiRowReader reader;

	/**
	 * CSV parser.
	 */
	private MappingIterator<T> parser = null;

	/**
	 * Constructor.
	 *
	 * @param objectReader CSV object mapper.
	 * @param lookahead    Lookahead.
	 */
	Jackson2CsvReactiveParser(ObjectReader objectReader, int lookahead) {
		Assert.notNull(objectReader, "objectReader must not be null");
		Assert.isTrue(lookahead > 0, "lookahead must be positive");

		this.objectReader = objectReader;
		this.lookahead = lookahead;
		this.reader = new MultiRowReader();
	}

	/**
	 * Parse a single CSV row.
	 */
	Mono<T> parse(String row) {
		Assert.notNull(row, "row must not be null");

		// Accumulate enough lookahead so that the parser below won't run out of rows.
		reader.addRow(row);
		if (reader.size() <= lookahead) {
			return Mono.empty();
		}

		try {
			if (parser == null) {
				parser = objectReader.readValues(reader);
			}

			return parser.hasNext() ?
					Mono.just(parser.next()) :
					Mono.empty();

		} catch (IOException | RuntimeException e) {
			// Shouldn't happen because the web client should handle all IO related errors upfront.
			return Mono.error(e);
		}
	}

	/**
	 * Parse the remaining rows in the reader without lookahead.
	 */
	Flux<T> parseRemaining() {
		reader.close();
		try {
			if (parser == null) {
				parser = objectReader.readValues(reader);
			}

			var rows = new ArrayList<T>(reader.size());
			parser.forEachRemaining(rows::add);
			return Flux.fromIterable(rows);

		} catch (IOException | RuntimeException e) {
			// Shouldn't happen because the web client should handle all IO related errors upfront.
			return Flux.error(e);
		}
	}

}
