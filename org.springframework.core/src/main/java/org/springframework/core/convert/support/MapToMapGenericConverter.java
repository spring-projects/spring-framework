package org.springframework.core.convert.support;

import java.util.Map;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.TypeDescriptor;

class MapToMapGenericConverter implements GenericConverter {

	private GenericConversionService conversionService;

	public MapToMapGenericConverter(GenericConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public Object convert(Object source, TypeDescriptor targetType) {
		Map sourceMap = (Map) source;
		Class targetKeyType = targetType.getMapKeyType();
		Class targetValueType = targetType.getMapValueType();
		if (targetKeyType == null && targetValueType == null) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		Class[] sourceEntryTypes = getMapEntryTypes(sourceMap);
		Class sourceKeyType = sourceEntryTypes[0];
		Class sourceValueType = sourceEntryTypes[1];
		if (sourceKeyType == null && sourceValueType == null) {
			return compatibleMapWithoutEntryConversion(sourceMap, targetType);
		}
		boolean keysCompatible = false;
		if (targetKeyType != null && sourceKeyType != null && targetKeyType.isAssignableFrom(sourceKeyType)) {
			keysCompatible = true;
		}
		boolean valuesCompatible = false;
		if (targetValueType != null && sourceValueType != null && targetValueType.isAssignableFrom(sourceValueType)) {
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

	private Class[] getMapEntryTypes(Map sourceMap) {
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
		return new Class[] { keyType, valueType };
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

		private TypeDescriptor targetKeyTypeDescriptor;

		private TypeDescriptor targetValueTypeDescriptor;

		public MapEntryConverter(Class sourceKeyType, Class sourceValueType, Class targetKeyType,
				Class targetValueType, boolean keysCompatible, boolean valuesCompatible,
				GenericConversionService conversionService) {
			if (sourceKeyType != null && targetKeyType != null && !keysCompatible) {
				this.targetKeyTypeDescriptor = TypeDescriptor.valueOf(targetKeyType);
				this.keyConverter = conversionService.getConverter(sourceKeyType, targetKeyTypeDescriptor);
			}
			if (sourceValueType != null && targetValueType != null && !valuesCompatible) {
				this.targetValueTypeDescriptor = TypeDescriptor.valueOf(targetValueType);
				this.valueConverter = conversionService.getConverter(sourceValueType, targetValueTypeDescriptor);
			}
		}

		public Object convertKey(Object sourceKey) {
			if (sourceKey != null && this.keyConverter != null) {
				return this.keyConverter.convert(sourceKey, targetKeyTypeDescriptor);
			} else {
				return sourceKey;
			}
		}

		public Object convertValue(Object sourceValue) {
			if (sourceValue != null && this.valueConverter != null) {
				return this.valueConverter.convert(sourceValue, targetValueTypeDescriptor);
			} else {
				return sourceValue;
			}
		}

	}

}
