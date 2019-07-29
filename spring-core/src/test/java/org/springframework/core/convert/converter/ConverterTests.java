package org.springframework.core.convert.converter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link Converter}
 *
 * @author Josh Cummings
 */
public class ConverterTests {
	Converter<Integer, Integer> moduloTwo = number -> number % 2;

	@Test
	public void andThenWhenGivenANullConverterThenThrowsException() {
		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> this.moduloTwo.andThen(null));
	}

	@Test
	public void andThenWhenGivenConverterThenComposesInOrder() {
		Converter<Integer, Integer> addOne = number-> number + 1;
		assertThat(this.moduloTwo.andThen(addOne).convert(13)).isEqualTo(2);
		assertThat(addOne.andThen(this.moduloTwo).convert(13)).isEqualTo(0);
	}
}