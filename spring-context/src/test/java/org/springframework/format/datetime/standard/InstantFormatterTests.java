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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static java.time.Instant.MAX;
import static java.time.Instant.MIN;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InstantFormatter}.
 *
 * @author Andrei Nevedomskii
 * @author Sam Brannen
 * @since 5.1.12
 */
@DisplayName("InstantFormatter unit tests")
@DisplayNameGeneration(ReplaceUnderscores.class)
class InstantFormatterTests {

	private final InstantFormatter instantFormatter = new InstantFormatter();

	@ParameterizedTest
	@ArgumentsSource(ISOSerializedInstantProvider.class)
	void should_parse_an_ISO_formatted_string_representation_of_an_Instant(String input) throws ParseException {
		Instant expected = DateTimeFormatter.ISO_INSTANT.parse(input, Instant::from);

		Instant actual = instantFormatter.parse(input, null);

		assertThat(actual).isEqualTo(expected);
	}

	@ParameterizedTest
	@ArgumentsSource(RFC1123SerializedInstantProvider.class)
	void should_parse_an_RFC1123_formatted_string_representation_of_an_Instant(String input) throws ParseException {
		Instant expected = DateTimeFormatter.RFC_1123_DATE_TIME.parse(input, Instant::from);

		Instant actual = instantFormatter.parse(input, null);

		assertThat(actual).isEqualTo(expected);
	}

	@ParameterizedTest
	@ArgumentsSource(RandomInstantProvider.class)
	void should_serialize_an_Instant_using_ISO_format_and_ignoring_Locale(Instant input) {
		String expected = DateTimeFormatter.ISO_INSTANT.format(input);

		String actual = instantFormatter.print(input, null);

		assertThat(actual).isEqualTo(expected);
	}

	private static class RandomInstantProvider implements ArgumentsProvider {

		private static final long DATA_SET_SIZE = 10;

		private static final Random random = new Random();

		@Override
		public final Stream<Arguments> provideArguments(ExtensionContext context) {
			return provideArguments().map(Arguments::of).limit(DATA_SET_SIZE);
		}

		Stream<?> provideArguments() {
			return randomInstantStream(MIN, MAX);
		}

		Stream<Instant> randomInstantStream(Instant min, Instant max) {
			return Stream.concat(Stream.of(Instant.now()), // make sure that the data set includes current instant
					random.longs(min.getEpochSecond(), max.getEpochSecond()).mapToObj(Instant::ofEpochSecond));
		}
	}

	private static class ISOSerializedInstantProvider extends RandomInstantProvider {

		@Override
		Stream<?> provideArguments() {
			return randomInstantStream(MIN, MAX).map(DateTimeFormatter.ISO_INSTANT::format);
		}
	}

	private static class RFC1123SerializedInstantProvider extends RandomInstantProvider {

		// RFC-1123 supports only 4-digit years
		private final Instant min = Instant.parse("0000-01-01T00:00:00.00Z");

		private final Instant max = Instant.parse("9999-12-31T23:59:59.99Z");

		@Override
		Stream<?> provideArguments() {
			return randomInstantStream(min, max)
					.map(DateTimeFormatter.RFC_1123_DATE_TIME.withZone(systemDefault())::format);
		}
	}

}
