package org.springframework.core.convert.support;

import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;

final class MapToMapGenericConverter implements GenericConverter {

	private final GenericConversionService conversionService;

	public MapToMapGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Map sourceMap = (Map) source;
		TypeDescriptor targetKeyType = targetType.getMapKeyTypeDescriptor();
		TypeDescriptor targetValueType = targetType.getMapValueTypeDescriptor();
		if (targetKeyType == null && targetValueType == null) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		TypeDescriptor[] sourceEntryTypes = getMapEntryTypes(sourceMap);
		TypeDescriptor sourceKeyType = sourceEntryTypes[0];
		TypeDescriptor sourceValueType = sourceEntryTypes[1];
		if (sourceKeyType == null && sourceValueType == null) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		boolean keysCompatible = false;
		if (sourceKeyType != TypeDescriptor.NULL && targetKeyType != TypeDescriptor.NULL && sourceKeyType.isAssignableTo(targetKeyType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (sourceValueType != TypeDescriptor.NULL && targetValueType != TypeDescriptor.NULL && sourceValueType.isAssignableTo(targetValueType)) {
			valuesCompatible = true;
		}
		if (keysCompatible && valuesCompatible) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		Map targetMap = CollectionFactory.createMap(targetType.getType(), sourceMap.size());
		MapEntryConverter converter = new MapEntryConverter(sourceKeyType, sourceValueType, targetKeyType, targetValueType, keysCompatible, valuesCompatible, conversionService);
		for (Object entry : sourceMap.entrySet()) {
			Map.Entry sourceMapEntry = (Map.Entry) entry;
			targetMap.put(converter.convertKey(sourceMapEntry.getKey()), converter.convertValue(sourceMapEntry.getValue()));
		}
		return targetMap;
	}

	private TypeDescriptor[] getMapEntryTypes(Map sourceMap) {
		Class keyType = null;
		Class valueType = null;
		for (Object entry : sourceMap.entrySet()) {
			Map.Entry mapEntry = (Map.Entry) entry;
			Object key = mapEntry.getKey();
			if (keyType == null && key != null) {
				keyType = key.getClass();
			}
			Object value = mapEntry.getValue();
			if (valueType == null && value != null) {
				valueType = value.getClass();
			}
			if (mapEntry.getKey() != null && mapEntry.getValue() != null) {
				break;
			}
		}
		return new TypeDescriptor[] { TypeDescriptor.valueOf(keyType), TypeDescriptor.valueOf(valueType) };
	}

	private Map compatibleMapWithoutEntryConversion(Map source, TypeDescriptor targetType) {
		if (targetType.getType().isAssignableFrom(source.getClass())) {
			return source;
		} else {
			Map target = CollectionFactory.createMap(targetType.getType(), source.size());
			target.putAll(source);
			return target;
		}
	}

	private static class MapEntryConverter {

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
				this.targetKeyType = targetKeyType;
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

}
