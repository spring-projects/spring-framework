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

package org.springframework.format.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Declares that a field or method parameter should be formatted as a {@link java.time.Duration},
 * according to the specified {@code style}.
 *
 * @author Simon Baslé
 * @since 6.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DurationFormat {

	/**
	 * Which {@code Style} to use for parsing and printing a {@code Duration}. Defaults to
	 * the JDK style ({@link Style#ISO8601}).
	 */
	Style style() default Style.ISO8601;

	/**
	 * Define which {@code ChronoUnit} to fall back to in case the {@code style()}
	 * needs a unit for either parsing or printing, and none is explicitly provided in
	 * the input.
	 */
	ChronoUnit defaultUnit() default ChronoUnit.MILLIS;

	/**
	 * Duration format styles.
	 */
	enum Style {

		/**
		 * Simple formatting based on a short suffix, for example '1s'.
		 * Supported unit suffixes are: {@code ns, us, ms, s, m, h, d}.
		 * This corresponds to nanoseconds, microseconds, milliseconds, seconds,
		 * minutes, hours and days respectively.
		 * <p>Note that when printing a {@code Duration}, this style can be lossy if the
		 * selected unit is bigger than the resolution of the duration. For example,
		 * {@code Duration.ofMillis(5).plusNanos(1234)} would get truncated to {@code "5ms"}
		 * when printing using {@code ChronoUnit.MILLIS}.
		 */
		SIMPLE,

		/**
		 * ISO-8601 formatting.
		 * <p>This is what the JDK uses in {@link java.time.Duration#parse(CharSequence)}
		 * and {@link Duration#toString()}.
		 */
		ISO8601;
	}

}
