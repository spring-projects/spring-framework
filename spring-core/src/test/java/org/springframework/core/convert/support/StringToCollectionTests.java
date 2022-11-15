/*
 * Copyright 2020-2022 the original author or authors.
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
