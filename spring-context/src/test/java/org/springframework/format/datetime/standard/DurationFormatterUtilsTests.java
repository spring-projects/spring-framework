/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.Arrays;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.format.annotation.DurationFormat.Unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.format.annotation.DurationFormat.Style.COMPOSITE;
import static org.springframework.format.annotation.DurationFormat.Style.ISO8601;
import static org.springframework.format.annotation.DurationFormat.Style.SIMPLE;

/**
 * Tests for {@link DurationFormatterUtils}.
 */
class DurationFormatterUtilsTests {

	@Test
	void parseSimpleWithUnits() {
		Duration nanos = DurationFormatterUtils.parse("1ns", SIMPLE, Unit.SECONDS);
		Duration micros = DurationFormatterUtils.parse("-2us", SIMPLE, Unit.SECONDS);
		Duration millis = DurationFormatterUtils.parse("+3ms", SIMPLE, Unit.SECONDS);
		Duration seconds = DurationFormatterUtils.parse("4s", SIMPLE, Unit.SECONDS);
		Duration minutes = DurationFormatterUtils.parse("5m", SIMPLE, Unit.SECONDS);
		Duration hours = DurationFormatterUtils.parse("6h", SIMPLE, Unit.SECONDS);
		Duration days = DurationFormatterUtils.parse("-10d", SIMPLE, Unit.SECONDS);

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
		assertThat(DurationFormatterUtils.parse("-123", SIMPLE, Unit.SECONDS))
				.hasSeconds(-123);
		assertThat(DurationFormatterUtils.parse("456", SIMPLE, Unit.SECONDS))
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
				.withCause(new IllegalArgumentException("'y' is not a valid simple duration Unit"));
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
		assertThat(DurationFormatterUtils.parse("P2DT3H4M", ISO8601, Unit.NANOS))
				.isEqualTo(Duration.ofDays(2).plusHours(3).plusMinutes(4));
	}

	@Test
	void parseIsoThrows() {
		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.parse("P2DWV3H-4M", ISO8601))
				.withMessage("'P2DWV3H-4M' is not a valid ISO-8601 duration")
				.withCause(new DateTimeParseException("Text cannot be parsed to a Duration", "", 0));
	}

	@Test
	void parseComposite() {
		assertThat(DurationFormatterUtils.parse("1d2h34m57s28ms3us2ns", COMPOSITE))
				.isEqualTo(Duration.ofDays(1).plusHours(2)
						.plusMinutes(34).plusSeconds(57)
						.plusMillis(28).plusNanos(3002));
	}

	@Test
	void parseCompositeWithExplicitPlusSign() {
		assertThat(DurationFormatterUtils.parse("+1d2h34m57s28ms3us2ns", COMPOSITE))
				.isEqualTo(Duration.ofDays(1).plusHours(2)
						.plusMinutes(34).plusSeconds(57)
						.plusMillis(28).plusNanos(3002));
	}

	@Test
	void parseCompositeWithExplicitMinusSign() {
		assertThat(DurationFormatterUtils.parse("-1d2h34m57s28ms3us2ns", COMPOSITE))
				.isEqualTo(Duration.ofDays(-1).plusHours(-2)
						.plusMinutes(-34).plusSeconds(-57)
						.plusMillis(-28).plusNanos(-3002));
	}

	@Test
	void parseCompositePartial() {
		assertThat(DurationFormatterUtils.parse("34m57s", COMPOSITE))
				.isEqualTo(Duration.ofMinutes(34).plusSeconds(57));
	}

	@Test
	void parseCompositePartialWithSpaces() {
		assertThat(DurationFormatterUtils.parse("34m 57s", COMPOSITE))
				.isEqualTo(Duration.ofMinutes(34).plusSeconds(57));
	}

	@Test //Kotlin style compatibility
	void parseCompositeNegativeWithSpacesAndParenthesis() {
		assertThat(DurationFormatterUtils.parse("-(34m 57s)", COMPOSITE))
				.isEqualTo(Duration.ofMinutes(-34).plusSeconds(-57));
	}

	@Test
	void parseCompositeBadSign() {
		assertThatException().isThrownBy(() -> DurationFormatterUtils.parse("+-34m57s", COMPOSITE))
				.havingCause().withMessage("Does not match composite duration pattern");
	}

	@Test
	void parseCompositeBadUnit() {
		assertThatException().isThrownBy(() -> DurationFormatterUtils.parse("34mo57s", COMPOSITE))
				.havingCause().withMessage("Does not match composite duration pattern");
	}

	@Test
	void printSimple() {
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(12345), SIMPLE, Unit.NANOS))
				.isEqualTo("12345ns");
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(-12345), SIMPLE, Unit.MICROS))
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
		assertThat(DurationFormatterUtils.print(Duration.ofNanos(12345), ISO8601, Unit.HOURS))
				.isEqualTo("PT0.000012345S");
		assertThat(DurationFormatterUtils.print(Duration.ofSeconds(-3), ISO8601, Unit.HOURS))
				.isEqualTo("PT-3S");
	}

	@Test
	void printCompositePositive() {
		Duration composite = DurationFormatterUtils.parse("+1d2h34m57s28ms3us2ns", COMPOSITE);
		assertThat(DurationFormatterUtils.print(composite, COMPOSITE))
				.isEqualTo("1d2h34m57s28ms3us2ns");
	}

	@Test
	void printCompositeZero() {
		assertThat(DurationFormatterUtils.print(Duration.ZERO, COMPOSITE))
				.isEqualTo("0s");
	}

	@Test
	void printCompositeNegative() {
		Duration composite = DurationFormatterUtils.parse("-1d2h34m57s28ms3us2ns", COMPOSITE);
		assertThat(DurationFormatterUtils.print(composite, COMPOSITE))
				.isEqualTo("-1d2h34m57s28ms3us2ns");
	}

	@Test
	void detectAndParse() {
		assertThat(DurationFormatterUtils.detectAndParse("PT1.234S", Unit.NANOS))
				.as("iso")
				.isEqualTo(Duration.ofMillis(1234));

		assertThat(DurationFormatterUtils.detectAndParse("1234ms", Unit.NANOS))
				.as("simple with explicit unit")
				.isEqualTo(Duration.ofMillis(1234));

		assertThat(DurationFormatterUtils.detectAndParse("1234", Unit.NANOS))
				.as("simple without suffix")
				.isEqualTo(Duration.ofNanos(1234));

		assertThat(DurationFormatterUtils.detectAndParse("3s45ms", Unit.NANOS))
				.as("composite")
				.isEqualTo(Duration.ofMillis(3045));
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

		assertThat(DurationFormatterUtils.detectAndParse("3s45ms"))
				.as("composite")
				.isEqualTo(Duration.ofMillis(3045));
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

		assertThat(DurationFormatterUtils.detect("-(1d 2h 34m 2ns)"))
				.as("COMPOSITE")
						.isEqualTo(COMPOSITE);

		assertThatIllegalArgumentException().isThrownBy(() -> DurationFormatterUtils.detect("WPT2H-4M"))
				.withMessage("'WPT2H-4M' is not a valid duration, cannot detect any known style")
				.withNoCause();
	}

	@Nested
	class DurationFormatUnit {

		@Test
		void longValueFromUnit() {
			Duration nanos = Duration.ofSeconds(3).plusMillis(22).plusNanos(1111);
			assertThat(Unit.NANOS.longValue(nanos))
					.as("NANOS")
					.isEqualTo(3022001111L);
			assertThat(Unit.MICROS.longValue(nanos))
					.as("MICROS")
					.isEqualTo(3022001);
			assertThat(Unit.MILLIS.longValue(nanos))
					.as("MILLIS")
					.isEqualTo(3022);
			assertThat(Unit.SECONDS.longValue(nanos))
					.as("SECONDS")
					.isEqualTo(3);

			Duration minutes = Duration.ofHours(1).plusMinutes(23);
			assertThat(Unit.MINUTES.longValue(minutes))
					.as("MINUTES")
					.isEqualTo(83);
			assertThat(Unit.HOURS.longValue(minutes))
					.as("HOURS")
					.isEqualTo(1);

			Duration days = Duration.ofHours(48 + 5);
			assertThat(Unit.HOURS.longValue(days))
					.as("HOURS in days")
					.isEqualTo(53);
			assertThat(Unit.DAYS.longValue(days))
					.as("DAYS")
					.isEqualTo(2);
		}

		@Test
		void unitFromSuffix() {
			assertThat(Unit.fromSuffix("ns")).as("ns").isEqualTo(Unit.NANOS);
			assertThat(Unit.fromSuffix("us")).as("us").isEqualTo(Unit.MICROS);
			assertThat(Unit.fromSuffix("ms")).as("ms").isEqualTo(Unit.MILLIS);
			assertThat(Unit.fromSuffix("s")).as("s").isEqualTo(Unit.SECONDS);
			assertThat(Unit.fromSuffix("m")).as("m").isEqualTo(Unit.MINUTES);
			assertThat(Unit.fromSuffix("h")).as("h").isEqualTo(Unit.HOURS);
			assertThat(Unit.fromSuffix("d")).as("d").isEqualTo(Unit.DAYS);

			assertThatIllegalArgumentException().isThrownBy(() -> Unit.fromSuffix("ws"))
					.withMessage("'ws' is not a valid simple duration Unit");
		}

		@Test
		void unitFromChronoUnit() {
			assertThat(Unit.fromChronoUnit(ChronoUnit.NANOS)).as("ns").isEqualTo(Unit.NANOS);
			assertThat(Unit.fromChronoUnit(ChronoUnit.MICROS)).as("us").isEqualTo(Unit.MICROS);
			assertThat(Unit.fromChronoUnit(ChronoUnit.MILLIS)).as("ms").isEqualTo(Unit.MILLIS);
			assertThat(Unit.fromChronoUnit(ChronoUnit.SECONDS)).as("s").isEqualTo(Unit.SECONDS);
			assertThat(Unit.fromChronoUnit(ChronoUnit.MINUTES)).as("m").isEqualTo(Unit.MINUTES);
			assertThat(Unit.fromChronoUnit(ChronoUnit.HOURS)).as("h").isEqualTo(Unit.HOURS);
			assertThat(Unit.fromChronoUnit(ChronoUnit.DAYS)).as("d").isEqualTo(Unit.DAYS);

			assertThatIllegalArgumentException().isThrownBy(() -> Unit.fromChronoUnit(ChronoUnit.CENTURIES))
					.withMessage("No matching Unit for ChronoUnit.CENTURIES");
		}

		@Test
		void unitSuffixSmokeTest() {
			assertThat(Arrays.stream(Unit.values()).map(u -> u.name() + "->" + u.asSuffix()))
					.containsExactly("NANOS->ns", "MICROS->us", "MILLIS->ms", "SECONDS->s",
							"MINUTES->m", "HOURS->h", "DAYS->d");
		}

		@Test
		void chronoUnitSmokeTest() {
			assertThat(Arrays.stream(Unit.values()).map(Unit::asChronoUnit))
					.containsExactly(ChronoUnit.NANOS, ChronoUnit.MICROS, ChronoUnit.MILLIS,
							ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS, ChronoUnit.DAYS);
		}
	}

}
