/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.springframework.core.convert.TypeDescriptor;

import static org.junit.Assert.*;

public class StringToCollectionConverterTests {

	private GenericConversionService conversionService = new GenericConversionService();

	public List<String> strings;

	@SuppressWarnings("unchecked")
	@Test
	/**
	 * See SPR-11126.
	 */
	public void emptyStringToList() throws Exception {
		conversionService.addConverter(new StringToCollectionConverter(conversionService));
		conversionService.addConverterFactory(new StringToNumberConverterFactory());
		String source = "";
		TypeDescriptor sourceType = TypeDescriptor.forObject(source);
		TypeDescriptor targetType = new TypeDescriptor(getClass().getField("strings"));
		assertTrue(conversionService.canConvert(sourceType, targetType));
		Collection<Object> result = (Collection<Object>)conversionService.convert(source, sourceType, targetType);
		assertEquals(1, result.size());
	}

}
