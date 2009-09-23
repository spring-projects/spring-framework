/**
 * 
 */
package org.springframework.core.convert.support;

import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

class MapEntryConverter {

	private GenericConverter keyConverter;

	private GenericConverter valueConverter;

	private TypeDescriptor sourceKeyType;

	private TypeDescriptor sourceValueType;
	
	private TypeDescriptor targetKeyType;
	
	private TypeDescriptor targetValueType;

	public MapEntryConverter(TypeDescriptor sourceKeyType, TypeDescriptor sourceValueType, TypeDescriptor targetKeyType,
			TypeDescriptor targetValueType, boolean keysCompatible, boolean valuesCompatible,
			GenericConversionService conversionService) {
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
			return invokeConverter(this.keyConverter, sourceKey, this.sourceKeyType, this.targetKeyType);
		} else {
			return sourceKey;
		}
	}

	public Object convertValue(Object sourceValue) {
		if (sourceValue != null && this.valueConverter != null) {
			return invokeConverter(this.valueConverter, sourceValue, this.sourceValueType, this.targetValueType);
		} else {
			return sourceValue;
		}
	}

}