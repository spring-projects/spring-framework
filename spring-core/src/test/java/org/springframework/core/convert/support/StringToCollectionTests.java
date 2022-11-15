package org.springframework.core.convert.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.convert.TypeDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

public class StringToCollectionTests {

	@Test
	void stringToCollectionWithCommaDelimiter() {
		List<String> expected = List.of("one", "two");
		GenericConversionService conversionService = new GenericConversionService();
		StringToCollectionConverter converter = new StringToCollectionConverter(conversionService);

		String source = "one,two";
		TypeDescriptor sourceType = TypeDescriptor.valueOf(String.class);
		TypeDescriptor targetType = TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class));

		List<String> actual = (List<String>) converter.convert(source, sourceType, targetType);

		assertThat(actual).isEqualTo(expected);
	}

}
