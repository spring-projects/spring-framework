package org.springframework.format.datetime.standard;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.text.ParseException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.stream.Stream;

import static java.time.Instant.MAX;
import static java.time.Instant.MIN;
import static java.time.ZoneId.systemDefault;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("ConstantConditions")
class InstantFormatterTests {

	private final InstantFormatter instantFormatter = new InstantFormatter();

	@ParameterizedTest
	@ArgumentsSource(ISOSerializedInstantProvider.class)
	void should_parse_an_ISO_formatted_string_representation_of_an_instant(final String input) throws ParseException {
		final Instant expected = DateTimeFormatter.ISO_INSTANT.parse(input, Instant::from);

		final Instant actual = instantFormatter.parse(input, null);

		assertThat(actual).isEqualTo(expected);
	}

	@ParameterizedTest
	@ArgumentsSource(RFC1123SerializedInstantProvider.class)
	void should_parse_an_RFC1123_formatted_string_representation_of_an_instant(final String input) throws ParseException {
		final Instant expected = DateTimeFormatter.RFC_1123_DATE_TIME.parse(input, Instant::from);

		final Instant actual = instantFormatter.parse(input, null);

		assertThat(actual).isEqualTo(expected);
	}

	@ParameterizedTest
	@ArgumentsSource(RandomInstantProvider.class)
	void should_serialize_an_instant_using_ISO_format_and_ignoring_locale(final Instant input) {
		final String expected = DateTimeFormatter.ISO_INSTANT.format(input);

		final String actual = instantFormatter.print(input, null);

		assertThat(actual).isEqualTo(expected);
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

	private static class RandomInstantProvider implements ArgumentsProvider {

		private static final long dataSetSize = 10;

		final Random random = new Random();

		Stream<?> provideArguments() {
			return randomInstantStream(MIN, MAX);
		}

		@Override
		public final Stream<? extends Arguments> provideArguments(ExtensionContext context) {
			return provideArguments().map(Arguments::of).limit(dataSetSize);
		}

		Stream<Instant> randomInstantStream(final Instant min, final Instant max) {
			return Stream.concat(
					Stream.of(Instant.now()), // make sure that the data set includes current instant
					random.longs(min.getEpochSecond(), max.getEpochSecond())
							.mapToObj(Instant::ofEpochSecond)
			);
		}
	}
}