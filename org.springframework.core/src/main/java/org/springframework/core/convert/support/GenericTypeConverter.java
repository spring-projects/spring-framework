/*
 * Copyright 2004-2008 the original author or authors.
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

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeConverter;
import org.springframework.core.convert.ConversionPoint;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterInfo;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.Assert;

/**
 * Base implementation of a conversion service. Initially empty, e.g. no converters are registered by default.
 * 
 * TODO - custom converters
 * TODO - object to collection/map converters
 * TODO - allow registration of converters to apply on presence of annotation values on setter or field e.g. String-to-@Mask String to apply a mask
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
public class GenericTypeConverter implements TypeConverter, ConverterRegistry {

	/**
	 * An indexed map of Converters. Each Map.Entry key is a source class (S) that can be converted from. Each Map.Entry
	 * value is a Map that defines the targetType-to-Converter mappings for that source.
	 */
	private final Map sourceTypeConverters = new HashMap();

	/**
	 * An optional parent conversion service.
	 */
	private TypeConverter parent;

	/**
	 * Returns the parent of this conversion service. Could be null.
	 */
	public TypeConverter getParent() {
		return parent;
	}

	/**
	 * Set the parent of this conversion service. This is optional.
	 */
	public void setParent(TypeConverter parent) {
		this.parent = parent;
	}

	/**
	 * Register the Converter with this conversion service.
	 * @param converter the converter to register
	 */
	public void addConverter(Converter converter) {
		List typeInfo = getRequiredTypeInfo(converter);
		Class sourceType = (Class) typeInfo.get(0);
		Class targetType = (Class) typeInfo.get(1);
		// index forward
		Map sourceMap = getSourceMap(sourceType);
		sourceMap.put(targetType, converter);
	}
	
	public void addConverterFactory(ConverterFactory<?, ?> converter) {
	}

	public void removeConverter(Converter<?, ?> converter) {
	}

	public void removeConverterFactory(Converter<?, ?> converter) {
	}	

	// implementing ConversionService


	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(sourceType, ConversionPoint.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, ConversionPoint<?> targetType) {
		ConversionExecutor executor = getConversionExecutor(sourceType, targetType);
		if (executor != null) {
			return true;
		} else {
			if (parent != null) {
				return parent.canConvert(sourceType, targetType);
			} else {
				return false;
			}
		}
	}

	public <S, T> T convert(S source, Class<T> targetType) {
		return convert(source, ConversionPoint.valueOf(targetType));
	}

	public <S, T> T convert(S source, ConversionPoint<T> targetType) {
		if (source == null) {
			return null;
		}
		if (source.getClass().isAssignableFrom(targetType.getType())) {
			return (T) source;
		}
		ConversionExecutor executor = getConversionExecutor(source.getClass(), targetType);
		if (executor != null) {
			return (T) executor.execute(source);
		} else {
			if (parent != null) {
				return parent.convert(source, targetType);
			} else {
				throw new ConverterNotFoundException(source.getClass(), targetType.getType(),
						"No converter found that can convert from sourceType [" + source.getClass().getName()
								+ "] to targetType [" + targetType.getName() + "]");
			}
		}
	}

	ConversionExecutor getConversionExecutor(Class sourceClass, ConversionPoint targetType)
			throws ConverterNotFoundException {
		Assert.notNull(sourceClass, "The sourceType to convert from is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
		ConversionPoint sourceType = ConversionPoint.valueOf(sourceClass);
		if (sourceType.isArray()) {
			if (targetType.isArray()) {
				return new ArrayToArray(sourceType, targetType, this);
			} else if (targetType.isCollection()) {
				if (targetType.isAbstractClass()) {
					throw new IllegalArgumentException("Conversion target class [" + targetType.getName()
							+ "] is invalid; cannot convert to abstract collection types--"
							+ "request an interface or concrete implementation instead");
				}
				return new ArrayToCollection(sourceType, targetType, this);
			}
		}
		if (targetType.isArray()) {
			if (sourceType.isCollection()) {
				return new CollectionToArray(sourceType, targetType, this);
			} else {
				return null;
			}
		}
		if (sourceType.isCollection()) {
			if (targetType.isCollection()) {
				return new CollectionToCollection(sourceType, targetType, this);
			} else {
				return null;
			}
		}
		if (sourceType.isMap()) {
			if (targetType.isMap()) {
				return new MapToMap(sourceType, targetType, this);
			} else {
				return null;
			}
		}
		Converter converter = findRegisteredConverter(sourceClass, targetType.getType());
		if (converter != null) {
			return new StaticConversionExecutor(sourceType, targetType, converter);
		} else {
			return null;
		}
	}

	// internal helpers

	private List getRequiredTypeInfo(Object converter) {
		List typeInfo = new ArrayList(2);
		if (converter instanceof ConverterInfo) {
			ConverterInfo info = (ConverterInfo) converter;
			typeInfo.add(info.getSourceType());
			typeInfo.add(info.getTargetType());
			return typeInfo;
		}
		Class classToIntrospect = converter.getClass();
		while (classToIntrospect != null) {
			Type[] genericInterfaces = classToIntrospect.getGenericInterfaces();
			for (Type genericInterface : genericInterfaces) {
				if (genericInterface instanceof ParameterizedType) {
					ParameterizedType pInterface = (ParameterizedType) genericInterface;
					if (Converter.class.isAssignableFrom((Class) pInterface.getRawType())) {
						Class s = getParameterClass(pInterface.getActualTypeArguments()[0], converter.getClass());
						Class t = getParameterClass(pInterface.getActualTypeArguments()[1], converter.getClass());
						typeInfo.add(getParameterClass(s, converter.getClass()));
						typeInfo.add(getParameterClass(t, converter.getClass()));
						break;
					}
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		if (typeInfo.size() != 2) {
			throw new IllegalArgumentException("Unable to extract source and target class arguments from Converter ["
					+ converter.getClass().getName() + "]; does the Converter specify the <S, T> generic types?");
		}
		return typeInfo;
	}

	private Class getParameterClass(Type parameterType, Class converterClass) {
		if (parameterType instanceof TypeVariable) {
			parameterType = GenericTypeResolver.resolveTypeVariable((TypeVariable) parameterType, converterClass);
		}
		if (parameterType instanceof Class) {
			return (Class) parameterType;
		}
		throw new IllegalArgumentException("Unable to obtain the java.lang.Class for parameterType [" + parameterType
				+ "] on Converter [" + converterClass.getName() + "]");
	}

	private Map getSourceMap(Class sourceType) {
		Map sourceMap = (Map) sourceTypeConverters.get(sourceType);
		if (sourceMap == null) {
			sourceMap = new HashMap();
			sourceTypeConverters.put(sourceType, sourceMap);
		}
		return sourceMap;
	}

	private Converter findRegisteredConverter(Class<?> sourceType, Class<?> targetType) {
		if (sourceType.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getConvertersForSource(currentClass);
				Converter converter = getConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			Map objectConverters = getConvertersForSource(Object.class);
			return getConverter(objectConverters, targetType);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getConvertersForSource(currentClass);
				Converter converter = getConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				if (currentClass.getSuperclass() != null) {
					classQueue.addFirst(currentClass.getSuperclass());
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			return null;
		}
	}

	private Map getConvertersForSource(Class sourceType) {
		Map converters = (Map) sourceTypeConverters.get(sourceType);
		return converters != null ? converters : Collections.emptyMap();
	}

	private Converter getConverter(Map converters, Class targetType) {
		return (Converter) converters.get(targetType);
	}

}