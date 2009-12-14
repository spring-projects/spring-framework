/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;

/**
 * Helper for converting map entries.
 *
 * @author Keith Donald
 * @since 3.0
 */
class MapEntryConverter {

	private GenericConverter keyConverter;

	private GenericConverter valueConverter;

	private TypeDescriptor sourceKeyType;

	private TypeDescriptor sourceValueType;

	private TypeDescriptor targetKeyType;

	private TypeDescriptor targetValueType;

	public MapEntryConverter(TypeDescriptor sourceKeyType, TypeDescriptor sourceValueType,
			TypeDescriptor targetKeyType, TypeDescriptor targetValueType, boolean keysCompatible,
			boolean valuesCompatible, GenericConversionService conversionService) {

		if (sourceKeyType != TypeDescriptor.NULL && targetKeyType != TypeDescriptor.NULL && !keysCompatible) {
			this.keyConverter = conversionService.getConverter(sourceKeyType, targetKeyType);
			if (this.keyConverter == null) {
				throw new ConverterNotFoundException(sourceKeyType, targetKeyType);
			}
			this.sourceKeyType = sourceKeyType;
			this.targetKeyType = targetKeyType;
		}

		if (sourceValueType != TypeDescriptor.NULL && targetValueType != TypeDescriptor.NULL && !valuesCompatible) {
			this.valueConverter = conversionService.getConverter(sourceValueType, targetValueType);
			if (this.valueConverter == null) {
				throw new ConverterNotFoundException(sourceValueType, targetValueType);
			}
			this.sourceValueType = sourceValueType;
			this.targetValueType = targetValueType;
		}
	}

	public Object convertKey(Object sourceKey) {
		if (sourceKey != null && this.keyConverter != null) {
			return ConversionUtils.invokeConverter(
					this.keyConverter, sourceKey, this.sourceKeyType, this.targetKeyType);
		}
		else {
			return sourceKey;
		}
	}

	public Object convertValue(Object sourceValue) {
		if (sourceValue != null && this.valueConverter != null) {
			return ConversionUtils.invokeConverter(
					this.valueConverter, sourceValue, this.sourceValueType, this.targetValueType);
		}
		else {
			return sourceValue;
		}
	}

}
