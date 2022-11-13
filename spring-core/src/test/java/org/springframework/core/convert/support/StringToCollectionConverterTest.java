package org.springframework.core.convert.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.TypeDescriptor;

import java.util.Collection;
import java.util.List;

public class StringToCollectionConverterTest {

	@Test
	void test() {
		Collection<String> expected = List.of("one,two");

		StringToSingletonCollectionConverter converter
				= new StringToSingletonCollectionConverter(new GenericConversionService());

		Collection<String> actual = (List<String>) converter.convert("one,two",
				TypeDescriptor.valueOf(String.class),
				TypeDescriptor.valueOf(List.class));

		Assertions.assertThat(actual).isEqualTo(expected);
	}

}
