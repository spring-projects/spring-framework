/*
 * Copyright 2002-2023 the original author or authors.
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

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.format.annotation.DurationFormat.Style.ISO8601;
import static org.springframework.format.annotation.DurationFormat.Style.SIMPLE;

class DurationFormatterUtilsTests {

	@Test
	void parseSimpleWithUnits() {
		Duration nanos = DurationFormatterUtils.parse("1ns", SIMPLE, ChronoUnit.SECONDS);
		Duration micros = DurationFormatterUtils.parse("-2us", SIMPLE, ChronoUnit.SECONDS);
		Duration millis = DurationFormatterUtils.parse("+3ms", SIMPLE, ChronoUnit.SECONDS);
		Duration seconds = DurationFormatterUtils.parse("4s", SIMPLE, ChronoUnit.SECONDS);
		Duration minutes = DurationFormatterUtils.parse("5m", SIMPLE, ChronoUnit.SECONDS);
		Duration hours = DurationFormatterUtils.parse("6h", SIMPLE, ChronoUnit.SECONDS);
		Duration days = DurationFormatterUtils.parse("-10d", SIMPLE, ChronoUnit.SECONDS);

		assertThat(nanos).hasNanos(1);
		assertThat(micros).hasNanos(-2 * 1000);
		assertThat(millis).hasMillis(3);
		assertThat(seconds).hasSeconds(4);
		assertThat(minutes).hasMinutes(5);
		assertThat(hours).hasHours(6);
		assertThat(days).hasDays(-10);
	}

	@Test
	void parseSimpleWithoutUnits() {
		assertThat(DurationFormatterUtils.parse("-123", SIMPLE, ChronoUnit.SECONDS))
				.hasSeconds(-123);
		assertThat(DurationFormatterUtils.parse("456", SIMPLE, ChronoUnit.SECONDS))
				.hasSeconds(456);
	}

	@Test
	void parseNoChronoUnitSimpleWithoutUnitsDefaultsToMillis() {
		assertThat(DurationFormatterUtils.parse("-123", SIMPLE))
				.hasMillis(-123);
		assertThat(DurationFormatterUtils.parse("456", SIMPLE))
				.hasMillis(456);
	}

	@Test
	void parseNullChronoUnitSimpleWithoutUnitsDefaultsToMillis() {
		assertThat(DurationFormatterUtils.parse("-123", SIMPLE, null))
				.hasMillis(-123);
		assertThat(DurationFormatterUtils.parse("456", SIMPLE, null))
				.hasMillis(456);
	}

	@Test
	void parseSimpleThrows() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.parse(";23s", SIMPLE))
				.withMessage("';23s' is not a valid simple duration")
				.withCause(new IllegalStateException("Does not match simple duration pattern"));

		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.parse("+23y", SIMPLE))
				.withMessage("'+23y' is not a valid simple duration")
				.withCause(new IllegalArgumentException("'y' is not a valid simple duration unit"));
	}

	@Test
	void parseIsoNoChronoUnit() {
		//these are based on the examples given in Duration.parse
//		"PT20.345S" -- parses as "20.345 seconds"
		assertThat(DurationFormatterUtils.parse("PT20.345S", ISO8601))
				.hasMillis(20345);
//     "PT15M"     -- parses as "15 minutes" (where a minute is 60 seconds)
		assertThat(DurationFormatterUtils.parse("PT15M", ISO8601))
				.hasSeconds(15*60);
//     "PT10H"     -- parses as "10 hours" (where an hour is 3600 seconds)
		assertThat(DurationFormatterUtils.parse("PT10H", ISO8601))
				.hasHours(10);
//     "P2D"       -- parses as "2 days" (where a day is 24 hours or 86400 seconds)
		assertThat(DurationFormatterUtils.parse("P2D", ISO8601))
				.hasDays(2);
//     "P2DT3H4M"  -- parses as "2 days, 3 hours and 4 minutes"
		assertThat(DurationFormatterUtils.parse("P2DT3H4M", ISO8601))
				.isEqualTo(Duration.ofDays(2).plusHours(3).plusMinutes(4));
//     "PT-6H3M"    -- parses as "-6 hours and +3 minutes"
		assertThat(DurationFormatterUtils.parse("PT-6H3M", ISO8601))
				.isEqualTo(Duration.ofHours(-6).plusMinutes(3));
//     "-PT6H3M"    -- parses as "-6 hours and -3 minutes"
		assertThat(DurationFormatterUtils.parse("-PT6H3M", ISO8601))
				.isEqualTo(Duration.ofHours(-6).plusMinutes(-3));
//     "-PT-6H+3M"  -- parses as "+6 hours and -3 minutes"
		assertThat(DurationFormatterUtils.parse("-PT-6H+3M", ISO8601))
				.isEqualTo(Duration.ofHours(6).plusMinutes(-3));
	}

	@Test
	void parseIsoIgnoresFallbackChronoUnit() {
		assertThat(DurationFormatterUtils.parse("P2DT3H4M", ISO8601, ChronoUnit.NANOS))
				.isEqualTo(Duration.ofDays(2).plusHours(3).plusMinutes(4));
	}

	@Test
	void parseIsoThrows() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.parse("P2DWV3H-4M", ISO8601))
				.withMessage("'P2DWV3H-4M' is not a valid ISO-8601 duration")
				.withCause(new DateTimeParseException("Text cannot be parsed to a Duration", "", 0));
	}

	@Test
	void printSimple() {
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(12345), SIMPLE, ChronoUnit.NANOS))
				.isEqualTo("12345ns");
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(-12345), SIMPLE, ChronoUnit.MICROS))
				.isEqualTo("-12us");
	}

	@Test
	void printSimpleNoChronoUnit() {
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(12345), SIMPLE))
				.isEqualTo("0ms");
		assertThat(DurationFormatterUtils.print(Duration.ofSeconds(-3), SIMPLE))
				.isEqualTo("-3000ms");
	}

	@Test
	void printIsoNoChronoUnit() {
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(12345), ISO8601))
				.isEqualTo("PT0.000012345S");
		assertThat(DurationFormatterUtils.print(Duration.ofSeconds(-3), ISO8601))
				.isEqualTo("PT-3S");
	}

	@Test
	void printIsoIgnoresChronoUnit() {
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(12345), ISO8601, ChronoUnit.HOURS))
				.isEqualTo("PT0.000012345S");
		assertThat(DurationFormatterUtils.print(Duration.ofSeconds(-3), ISO8601, ChronoUnit.HOURS))
				.isEqualTo("PT-3S");
	}

	@Test
	void detectAndParse() {
		assertThat(DurationFormatterUtils.detectAndParse("PT1.234S", ChronoUnit.NANOS))
				.as("iso")
				.isEqualTo(Duration.ofMillis(1234));

		assertThat(DurationFormatterUtils.detectAndParse("1234ms", ChronoUnit.NANOS))
				.as("simple with explicit unit")
				.isEqualTo(Duration.ofMillis(1234));

		assertThat(DurationFormatterUtils.detectAndParse("1234", ChronoUnit.NANOS))
				.as("simple without suffix")
				.isEqualTo(Duration.ofNanos(1234));
	}

	@Test
	void detectAndParseNoChronoUnit() {
		assertThat(DurationFormatterUtils.detectAndParse("PT1.234S"))
				.as("iso")
				.isEqualTo(Duration.ofMillis(1234));

		assertThat(DurationFormatterUtils.detectAndParse("1234ms"))
				.as("simple with explicit unit")
				.isEqualTo(Duration.ofMillis(1234));

		assertThat(DurationFormatterUtils.detectAndParse("1234"))
				.as("simple without suffix")
				.isEqualTo(Duration.ofMillis(1234));
	}

	@Test
	void detect() {
		assertThat(DurationFormatterUtils.detect("+3ms"))
				.as("SIMPLE")
				.isEqualTo(SIMPLE);
		assertThat(DurationFormatterUtils.detect("-10y"))
				.as("invalid yet matching SIMPLE pattern")
				.isEqualTo(SIMPLE);

		assertThat(DurationFormatterUtils.detect("P2DT3H-4M"))
				.as("ISO8601")
				.isEqualTo(ISO8601);
		assertThat(DurationFormatterUtils.detect("P2DWV3H-4M"))
				.as("invalid yet matching ISO8601 pattern")
				.isEqualTo(ISO8601);

		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.detect("WPT2H-4M"))
				.withMessage("'WPT2H-4M' is not a valid duration, cannot detect any known style")
				.withNoCause();
	}

	@Test
	void longValueFromUnit() {
		Duration nanos = Duration.ofSeconds(3).plusMillis(22).plusNanos(1111);
		assertThat(DurationFormatterUtils.longValueFromUnit(nanos, ChronoUnit.NANOS))
				.as("NANOS")
				.isEqualTo(3022001111L);
		assertThat(DurationFormatterUtils.longValueFromUnit(nanos, ChronoUnit.MICROS))
				.as("MICROS")
				.isEqualTo(3022001);
		assertThat(DurationFormatterUtils.longValueFromUnit(nanos, ChronoUnit.MILLIS))
				.as("MILLIS")
				.isEqualTo(3022);
		assertThat(DurationFormatterUtils.longValueFromUnit(nanos, ChronoUnit.SECONDS))
				.as("SECONDS")
				.isEqualTo(3);

		Duration minutes = Duration.ofHours(1).plusMinutes(23);
		assertThat(DurationFormatterUtils.longValueFromUnit(minutes, ChronoUnit.MINUTES))
				.as("MINUTES")
				.isEqualTo(83);
		assertThat(DurationFormatterUtils.longValueFromUnit(minutes, ChronoUnit.HOURS))
				.as("HOURS")
				.isEqualTo(1);

		Duration days = Duration.ofHours(48 + 5);
		assertThat(DurationFormatterUtils.longValueFromUnit(days, ChronoUnit.HOURS))
				.as("HOURS in days")
				.isEqualTo(53);
		assertThat(DurationFormatterUtils.longValueFromUnit(days, ChronoUnit.DAYS))
				.as("DAYS")
				.isEqualTo(2);
	}

	@Test
	void longValueFromUnsupportedUnit() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.longValueFromUnit(Duration.ofDays(3),
						ChronoUnit.HALF_DAYS)).as("HALF_DAYS")
				.withMessage("'HALF_DAYS' is not a supported ChronoUnit for simple duration representation");
		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.longValueFromUnit(Duration.ofDays(23),
						ChronoUnit.WEEKS)).as("WEEKS")
				.withMessage("'WEEKS' is not a supported ChronoUnit for simple duration representation");
	}

	@Test
	void unitFromSuffix() {
		assertThat(DurationFormatterUtils.unitFromSuffix("ns")).as("ns").isEqualTo(ChronoUnit.NANOS);
		assertThat(DurationFormatterUtils.unitFromSuffix("us")).as("us").isEqualTo(ChronoUnit.MICROS);
		assertThat(DurationFormatterUtils.unitFromSuffix("ms")).as("ms").isEqualTo(ChronoUnit.MILLIS);
		assertThat(DurationFormatterUtils.unitFromSuffix("s")).as("s").isEqualTo(ChronoUnit.SECONDS);
		assertThat(DurationFormatterUtils.unitFromSuffix("m")).as("m").isEqualTo(ChronoUnit.MINUTES);
		assertThat(DurationFormatterUtils.unitFromSuffix("h")).as("h").isEqualTo(ChronoUnit.HOURS);
		assertThat(DurationFormatterUtils.unitFromSuffix("d")).as("d").isEqualTo(ChronoUnit.DAYS);

		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.unitFromSuffix("ws"))
				.withMessage("'ws' is not a valid simple duration unit");
	}

	@Test
	void suffixFromUnit() {
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.NANOS)).as("NANOS").isEqualTo("ns");
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.MICROS)).as("MICROS").isEqualTo("us");
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.MILLIS)).as("MILLIS").isEqualTo("ms");
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.SECONDS)).as("SECONDS").isEqualTo("s");
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.MINUTES)).as("MINUTES").isEqualTo("m");
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.HOURS)).as("HOURS").isEqualTo("h");
		assertThat(DurationFormatterUtils.suffixFromUnit(ChronoUnit.DAYS)).as("DAYS").isEqualTo("d");

		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.suffixFromUnit(ChronoUnit.MILLENNIA))
				.withMessage("'MILLENNIA' is not a supported ChronoUnit for simple duration representation");
	}
}
