/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Converter}
 *
 * @author Josh Cummings
 * @author Sam Brannen
 * @since 5.3
 */
class ConverterTests {

	private final Converter<Integer, Integer> moduloTwo = number -> number % 2;
	private final Converter<Integer, Integer> addOne = number -> number + 1;


	@Test
	void andThenWhenGivenANullConverterThenThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.moduloTwo.andThen(null));
	}

	@Test
	void andThenWhenGivenConverterThenComposesInOrder() {
		assertThat(this.moduloTwo.andThen(this.addOne).convert(13)).isEqualTo(2);
		assertThat(this.addOne.andThen(this.moduloTwo).convert(13)).isEqualTo(0);
	}

	@Test
	void andThenCanConvertFromDifferentSourceType() {
		Converter<String, Integer> length = String::length;
		assertThat(length.andThen(this.moduloTwo).convert("example")).isEqualTo(1);
		assertThat(length.andThen(this.addOne).convert("example")).isEqualTo(8);
	}

	@Test
	void andThenCanConvertToDifferentTargetType() {
		Converter<String, Integer> length = String::length;
		Converter<Integer, String> toString = Object::toString;
		assertThat(length.andThen(toString).convert("example")).isEqualTo("7");
		assertThat(toString.andThen(length).convert(1_000)).isEqualTo(4);
	}

}
