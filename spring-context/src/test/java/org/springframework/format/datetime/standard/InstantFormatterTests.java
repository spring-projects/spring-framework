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

package org.springframework.format.datetime.standard;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Stream;

import org.junit.Test;

import static java.time.Instant.MAX;
import static java.time.Instant.MIN;
import static java.time.ZoneId.systemDefault;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link InstantFormatter}.
 *
 * @author Andrei Nevedomskii
 * @author Sam Brannen
 * @since 5.1.12
 */
public class InstantFormatterTests {

	private final InstantFormatter instantFormatter = new InstantFormatter();


	@Test
	public void should_parse_an_ISO_formatted_string_representation_of_an_Instant() {
		new ISOSerializedInstantProvider().provideArguments().forEach(input -> {
			try {
				Instant expected = DateTimeFormatter.ISO_INSTANT.parse(input, Instant::from);

				Instant actual = instantFormatter.parse(input, null);

				assertEquals(expected, actual);
			}
			catch (ParseException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Test
	public void should_parse_an_RFC1123_formatted_string_representation_of_an_Instant() {
		new RFC1123SerializedInstantProvider().provideArguments().forEach(input -> {
			try {
				Instant expected = DateTimeFormatter.RFC_1123_DATE_TIME.parse(input, Instant::from);

				Instant actual = instantFormatter.parse(input, null);

				assertEquals(expected, actual);
			}
			catch (ParseException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	@Test
	public void should_serialize_an_Instant_using_ISO_format_and_ignoring_Locale() {
		new RandomInstantProvider().randomInstantStream(MIN, MAX).forEach(instant -> {
			String expected = DateTimeFormatter.ISO_INSTANT.format(instant);

			String actual = instantFormatter.print(instant, null);

			assertEquals(expected, actual);
		});
	}


	private static class RandomInstantProvider {

		private static final long DATA_SET_SIZE = 10;

		private static final Random random = new Random();

		Stream<Instant> randomInstantStream(Instant min, Instant max) {
			return Stream.concat(Stream.of(Instant.now()), // make sure that the data set includes current instant
				random.longs(min.getEpochSecond(), max.getEpochSecond()).limit(DATA_SET_SIZE).mapToObj(Instant::ofEpochSecond));
		}
	}

	private static class ISOSerializedInstantProvider extends RandomInstantProvider {

		Stream<String> provideArguments() {
			return randomInstantStream(MIN, MAX).map(DateTimeFormatter.ISO_INSTANT::format);
		}
	}

	private static class RFC1123SerializedInstantProvider extends RandomInstantProvider {

		// RFC-1123 supports only 4-digit years
		private static final Instant min = Instant.parse("0000-01-01T00:00:00.00Z");

		private static final Instant max = Instant.parse("9999-12-31T23:59:59.99Z");


		Stream<String> provideArguments() {
			return randomInstantStream(min, max)
					.map(DateTimeFormatter.RFC_1123_DATE_TIME.withZone(systemDefault())::format);
		}
	}

}
