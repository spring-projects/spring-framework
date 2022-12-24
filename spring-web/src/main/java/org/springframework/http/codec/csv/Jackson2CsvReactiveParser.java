package org.springframework.http.codec.csv;

import java.io.IOException;
import java.util.ArrayList;

import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
	 * @param objectReader
	 * 		CSV object mapper.
	 * @param lookahead
	 * 		Lookahead.
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
