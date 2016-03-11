/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

/**
 * Calls {@link Enum#ordinal()} to convert a source Enum to a Integer.
 * This converter will not match enums with interfaces that can be converted.
 *
 * @author Yanming Zhou (zhouyanming@gmail.com)
 * @since 4.3
 */
final class EnumToIntegerConverter implements Converter<Enum<?>, Integer>, ConditionalConverter {

	private final ConversionService conversionService;


	public EnumToIntegerConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		for (Class<?> interfaceType : ClassUtils.getAllInterfacesForClass(sourceType.getType())) {
			if (this.conversionService.canConvert(TypeDescriptor.valueOf(interfaceType), targetType)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public Integer convert(Enum<?> source) {
		return source.ordinal();
	}

}
