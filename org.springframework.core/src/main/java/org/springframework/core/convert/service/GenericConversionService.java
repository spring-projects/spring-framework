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
package org.springframework.core.convert.service;

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
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionExecutorNotFoundException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterInfo;
import org.springframework.core.convert.converter.SuperConverter;
import org.springframework.core.convert.converter.SuperTwoWayConverter;
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
public class GenericConversionService implements ConversionService {

	/**
	 * An indexed map of Converters. Each Map.Entry key is a source class (S) that can be converted from. Each Map.Entry
	 * value is a Map that defines the targetType-to-Converter mappings for that source.
	 */
	private final Map sourceTypeConverters = new HashMap();

	/**
	 * An indexed map of SuperConverters. Each Map.Entry key is a source class (S) that can be converted from. Each
	 * Map.Entry value is a Map that defines the targetType-to-SuperConverter mappings for that source.
	 */
	private final Map sourceTypeSuperConverters = new HashMap();

	/**
	 * Indexes classes by well-known aliases.
	 */
	private final Map aliasMap = new HashMap<String, Class<?>>();

	/**
	 * An optional parent conversion service.
	 */
	private ConversionService parent;

	/**
	 * Returns the parent of this conversion service. Could be null.
	 */
	public ConversionService getParent() {
		return parent;
	}

	/**
	 * Set the parent of this conversion service. This is optional.
	 */
	public void setParent(ConversionService parent) {
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
		// index reverse
		sourceMap = getSourceMap(targetType);
		sourceMap.put(sourceType, new ReverseConverter(converter));
	}

	/**
	 * Register the SuperConverter with this conversion service.
	 * @param converter the super converter to register
	 */
	public void addConverter(SuperConverter converter) {
		List typeInfo = getRequiredTypeInfo(converter);
		Class sourceType = (Class) typeInfo.get(0);
		Class targetType = (Class) typeInfo.get(1);
		// index forward
		Map sourceMap = getSourceSuperConverterMap(sourceType);
		sourceMap.put(targetType, converter);
		if (converter instanceof SuperTwoWayConverter) {
			// index reverse
			sourceMap = getSourceSuperConverterMap(targetType);
			sourceMap.put(sourceType, new ReverseSuperConverter((SuperTwoWayConverter) converter));
		}
	}

	/**
	 * Adapts a {@link SuperTwoWayConverter} that converts between BS and BT class hierarchies to a {@link Converter}
	 * that converts between the specific BS/BT sub types S and T.
	 * @param sourceType the source class S to convert from, which must be equal or extend BS
	 * @param targetType the target type T to convert to, which must equal or extend BT
	 * @param converter the super two way converter
	 * @return a converter that converts from S to T by delegating to the super converter
	 */
	public static <S, T> Converter<S, T> converterFor(Class<S> sourceType, Class<T> targetType,
			SuperTwoWayConverter converter) {
		return new SuperTwoWayConverterConverter(converter, sourceType, targetType);
	}

	/**
	 * Add a convenient alias for the target type. {@link #getType(String)} can then be used to lookup the type given
	 * the alias.
	 * @see #getType(String)
	 */
	public void addAlias(String alias, Class targetType) {
		aliasMap.put(alias, targetType);
	}

	// implementing ConversionService

	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
		try {
			getConversionExecutor(sourceType, targetType);
			return true;
		} catch (ConversionExecutorNotFoundException e) {
			return false;
		}
	}

	public boolean canConvert(Object source, TypeDescriptor targetType) {
		if (source == null) {
			return true;
		}
		try {
			getConversionExecutor(source.getClass(), targetType);
			return true;
		} catch (ConversionExecutorNotFoundException e) {
			return false;
		}
	}

	public Object executeConversion(Object source, TypeDescriptor targetType)
			throws ConversionExecutorNotFoundException, ConversionException {
		if (source == null) {
			return null;
		}
		return getConversionExecutor(source.getClass(), targetType).execute(source);
	}

	public ConversionExecutor getConversionExecutor(Class sourceClass, TypeDescriptor targetType)
			throws ConversionExecutorNotFoundException {
		Assert.notNull(sourceClass, "The sourceType to convert from is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
		TypeDescriptor sourceType = TypeDescriptor.valueOf(sourceClass);
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
				throw new ConversionExecutorNotFoundException(sourceType, targetType, "Object to Array conversion not yet supported");
			}
		}
		if (sourceType.isCollection()) {
			if (targetType.isCollection()) {
				return new CollectionToCollection(sourceType, targetType, this);
			} else {
				throw new ConversionExecutorNotFoundException(sourceType, targetType, "Object to Collection conversion not yet supported");
			}
		}
		if (sourceType.isMap()) {
			if (targetType.isMap()) {
				return new MapToMap(sourceType, targetType, this);
			} else {
				throw new ConversionExecutorNotFoundException(sourceType, targetType, "Object to Map conversion not yet supported");
			}
		}
		Converter converter = findRegisteredConverter(sourceClass, targetType.getType());
		if (converter != null) {
			return new StaticConversionExecutor(sourceType, targetType, converter);
		} else {
			SuperConverter superConverter = findRegisteredSuperConverter(sourceClass, targetType.getType());
			if (superConverter != null) {
				return new StaticSuperConversionExecutor(sourceType, targetType, superConverter);
			}
			if (parent != null) {
				return parent.getConversionExecutor(sourceClass, targetType);
			} else {
				if (sourceType.isAssignableTo(targetType)) {
					return new StaticConversionExecutor(sourceType, targetType, NoOpConverter.INSTANCE);
				}
				throw new ConversionExecutorNotFoundException(sourceType, targetType,
						"No ConversionExecutor found for converting from sourceType [" + sourceType.getName()
								+ "] to targetType [" + targetType.getName() + "]");
			}
		}
	}

	public Class getType(String name) throws IllegalArgumentException {
		Class clazz = (Class) aliasMap.get(name);
		if (clazz != null) {
			return clazz;
		} else {
			if (parent != null) {
				return parent.getType(name);
			} else {
				return null;
			}
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
					if (Converter.class.equals(pInterface.getRawType())
							|| SuperConverter.class.isAssignableFrom((Class) pInterface.getRawType())) {
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

	private Map getSourceSuperConverterMap(Class sourceType) {
		Map sourceMap = (Map) sourceTypeSuperConverters.get(sourceType);
		if (sourceMap == null) {
			sourceMap = new HashMap();
			sourceTypeSuperConverters.put(sourceType, sourceMap);
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

	private SuperConverter findRegisteredSuperConverter(Class<?> sourceType, Class<?> targetType) {
		if (sourceType.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getSuperConvertersForSource(currentClass);
				SuperConverter converter = findSuperConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			Map objectConverters = getSuperConvertersForSource(Object.class);
			return findSuperConverter(objectConverters, targetType);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getSuperConvertersForSource(currentClass);
				SuperConverter converter = findSuperConverter(converters, targetType);
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

	private Map getSuperConvertersForSource(Class sourceType) {
		Map converters = (Map) sourceTypeSuperConverters.get(sourceType);
		return converters != null ? converters : Collections.emptyMap();
	}

	private SuperConverter findSuperConverter(Map converters, Class targetType) {
		if (converters.isEmpty()) {
			return null;
		}
		if (targetType.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				SuperConverter converter = (SuperConverter) converters.get(currentClass);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			return (SuperConverter) converters.get(Object.class);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				SuperConverter converter = (SuperConverter) converters.get(currentClass);
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

}