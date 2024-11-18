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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.function.Function;

import org.springframework.lang.Nullable;

/**
 * Declares that a field or method parameter should be formatted as a
 * {@link java.time.Duration}, according to the specified {@link #style Style}
 * and {@link #defaultUnit Unit}.
 *
 * @author Simon Baslé
 * @since 6.2
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DurationFormat {

	/**
	 * The {@link Style} to use for parsing and printing a {@link Duration}.
	 * <p>Defaults to the JDK style ({@link Style#ISO8601}).
	 */
	Style style() default Style.ISO8601;

	/**
	 * The {@link Unit} to fall back to in case the {@link #style Style} needs a unit
	 * for either parsing or printing, and none is explicitly provided in the input.
	 * <p>Defaults to {@link Unit#MILLIS} if unspecified.
	 */
	Unit defaultUnit() default Unit.MILLIS;

	/**
	 * {@link Duration} format styles.
	 */
	enum Style {

		/**
		 * Simple formatting based on a short suffix, for example '1s'.
		 * <p>Supported unit suffixes include: {@code ns, us, ms, s, m, h, d}.
		 * Those correspond to nanoseconds, microseconds, milliseconds, seconds,
		 * minutes, hours, and days, respectively.
		 * <p>Note that when printing a {@link Duration}, this style can be
		 * lossy if the selected unit is bigger than the resolution of the
		 * duration. For example, {@code Duration.ofMillis(5).plusNanos(1234)}
		 * would get truncated to {@code "5ms"} when printing using
		 * {@code ChronoUnit.MILLIS}.
		 * <p>Fractional durations are not supported.
		 */
		SIMPLE,

		/**
		 * ISO-8601 formatting.
		 * <p>This is what the JDK uses in {@link Duration#parse(CharSequence)}
		 * and {@link Duration#toString()}.
		 */
		ISO8601,

		/**
		 * Like {@link #SIMPLE}, but allows multiple segments ordered from
		 * largest-to-smallest units of time, like {@code 1h12m27s}.
		 * <p>A single minus sign ({@code -}) is allowed to indicate the whole
		 * duration is negative. Spaces are allowed between segments, and a
		 * negative duration with spaced segments can optionally be surrounded
		 * by parentheses after the minus sign, like so: {@code -(34m 57s)}.
		 */
		COMPOSITE
	}

	/**
	 * {@link Duration} format unit, which mirrors a subset of {@link ChronoUnit} and
	 * allows conversion to and from a supported {@code ChronoUnit} as well as
	 * conversion from durations to longs.
	 *
	 * <p>The enum includes its corresponding suffix in the {@link Style#SIMPLE SIMPLE}
	 * {@code Duration} format style.
	 */
	enum Unit {

		/**
		 * Nanoseconds ({@code "ns"}).
		 */
		NANOS(ChronoUnit.NANOS, "ns", Duration::toNanos),

		/**
		 * Microseconds ({@code "us"}).
		 */
		MICROS(ChronoUnit.MICROS, "us", duration -> duration.toNanos() / 1000L),

		/**
		 * Milliseconds ({@code "ms"}).
		 */
		MILLIS(ChronoUnit.MILLIS, "ms", Duration::toMillis),

		/**
		 * Seconds ({@code "s"}).
		 */
		SECONDS(ChronoUnit.SECONDS, "s", Duration::toSeconds),

		/**
		 * Minutes ({@code "m"}).
		 */
		MINUTES(ChronoUnit.MINUTES, "m", Duration::toMinutes),

		/**
		 * Hours ({@code "h"}).
		 */
		HOURS(ChronoUnit.HOURS, "h", Duration::toHours),

		/**
		 * Days ({@code "d"}).
		 */
		DAYS(ChronoUnit.DAYS, "d", Duration::toDays);

		private final ChronoUnit chronoUnit;

		private final String suffix;

		private final Function<Duration, Long> longValue;

		Unit(ChronoUnit chronoUnit, String suffix, Function<Duration, Long> toUnit) {
			this.chronoUnit = chronoUnit;
			this.suffix = suffix;
			this.longValue = toUnit;
		}

		/**
		 * Convert this {@code Unit} to its {@link ChronoUnit} equivalent.
		 */
		public ChronoUnit asChronoUnit() {
			return this.chronoUnit;
		}

		/**
		 * Convert this {@code Unit} to a simple {@code String} suffix, suitable
		 * for the {@link Style#SIMPLE SIMPLE} style.
		 */
		public String asSuffix() {
			return this.suffix;
		}

		/**
		 * Parse a {@code long} from the given {@link String} and interpret it to be a
		 * {@link Duration} in the current unit.
		 * @param value the {@code String} representation of the long
		 * @return the corresponding {@code Duration}
		 */
		public Duration parse(String value) {
			return Duration.of(Long.parseLong(value), asChronoUnit());
		}

		/**
		 * Print the given {@link Duration} as a {@link String}, converting it to a long
		 * value using this unit's precision via {@link #longValue(Duration)}
		 * and appending this unit's simple {@link #asSuffix() suffix}.
		 * @param value the {@code Duration} to convert to a {@code String}
		 * @return the {@code String} representation of the {@code Duration} in the
		 * {@link Style#SIMPLE SIMPLE} style
		 */
		public String print(Duration value) {
			return longValue(value) + asSuffix();
		}

		/**
		 * Convert the given {@link Duration} to a long value in the resolution
		 * of this unit.
		 * <p>Note that this can be lossy if the current unit is bigger than the
		 * actual resolution of the duration. For example,
		 * {@code Duration.ofMillis(5).plusNanos(1234)} would get truncated to
		 * {@code 5} for unit {@code MILLIS}.
		 * @param value the {@code Duration} to convert to a long
		 * @return the long value for the {@code Duration} in this {@code Unit}
		 */
		public long longValue(Duration value) {
			return this.longValue.apply(value);
		}

		/**
		 * Get the {@link Unit} corresponding to the given {@link ChronoUnit}.
		 * @throws IllegalArgumentException if the given {@code ChronoUnit} is
		 * not supported
		 */
		public static Unit fromChronoUnit(@Nullable ChronoUnit chronoUnit) {
			if (chronoUnit == null) {
				return Unit.MILLIS;
			}
			for (Unit candidate : values()) {
				if (candidate.chronoUnit == chronoUnit) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("No matching Unit for ChronoUnit." + chronoUnit.name());
		}

		/**
		 * Get the {@link Unit} corresponding to the given {@link String} suffix.
		 * @throws IllegalArgumentException if the given suffix is not supported
		 */
		public static Unit fromSuffix(String suffix) {
			for (Unit candidate : values()) {
				if (candidate.suffix.equalsIgnoreCase(suffix)) {
					return candidate;
				}
			}
			throw new IllegalArgumentException("'" + suffix + "' is not a valid simple duration Unit");
		}

	}

}
