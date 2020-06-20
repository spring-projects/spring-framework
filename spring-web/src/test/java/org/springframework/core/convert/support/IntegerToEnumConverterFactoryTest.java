package org.springframework.core.convert.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IntegerToEnumConverterFactoryTest {


	enum Colors {
		RED,
		BLUE,
		GREEN
	}

	@Test
	void convertIntegerToEnum() {
		final IntegerToEnumConverterFactory enumConverterFactory = new IntegerToEnumConverterFactory();
		assertEquals(Colors.RED, enumConverterFactory.getConverter(Colors.class).convert(0));
		assertEquals(Colors.BLUE, enumConverterFactory.getConverter(Colors.class).convert(1));
		assertEquals(Colors.GREEN, enumConverterFactory.getConverter(Colors.class).convert(2));
	}

	@Test
	void throwsArrayIndexOutOfBoundsExceptionIfInvalidEnumInteger() {
		final IntegerToEnumConverterFactory enumConverterFactory = new IntegerToEnumConverterFactory();
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> enumConverterFactory.getConverter(Colors.class).convert(999));
	}


}
