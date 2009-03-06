/*
 * Copyright 2004-2009 the original author or authors.
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

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
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
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.SuperConverter;
import org.springframework.util.Assert;

/**
 * Base implementation of a conversion service. Initially empty, e.g. no converters are registered by default.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
public class GenericConversionService implements ConversionService {

	/**
	 * An indexed map of Converters. Each Map.Entry key is a source class (S) that can be converted from. Each Map.Entry
	 * value is a Map that defines the targetClass-to-Converter mappings for that source.
	 */
	private final Map sourceClassConverters = new HashMap();

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
		List typeInfo = getTypeInfo(converter);
		Class sourceClass = (Class) typeInfo.get(0);
		Class targetClass = (Class) typeInfo.get(1);
		// index forward
		Map sourceMap = getSourceMap(sourceClass);
		sourceMap.put(targetClass, converter);
		// index reverse
		sourceMap = getSourceMap(targetClass);
		sourceMap.put(sourceClass, new ReverseConverter(converter));
	}

	/**
	 * Register the SuperConverter with this conversion service.
	 * @param converter the super converter to register
	 */
	public void addConverter(SuperConverter converter) {
		// TODO
	}

	/**
	 * Add a convenient alias for the target type. {@link #getClassForAlias(String)} can then be used to lookup the type
	 * given the alias.
	 * @see #getClassForAlias(String)
	 */
	public void addAlias(String alias, Class targetType) {
		aliasMap.put(alias, targetType);
	}

	public Object executeConversion(Object source, Class targetClass) throws ConversionExecutorNotFoundException,
			ConversionException {
		if (source != null) {
			ConversionExecutor conversionExecutor = getConversionExecutor(source.getClass(), targetClass);
			return conversionExecutor.execute(source);
		} else {
			return null;
		}
	}

	public Object executeConversion(String converterId, Object source, Class targetClass)
			throws ConversionExecutorNotFoundException, ConversionException {
		if (source != null) {
			ConversionExecutor conversionExecutor = getConversionExecutor(converterId, source.getClass(), targetClass);
			return conversionExecutor.execute(source);
		} else {
			return null;
		}
	}

	public ConversionExecutor getConversionExecutor(Class sourceClass, Class targetClass)
			throws ConversionExecutorNotFoundException {
		Assert.notNull(sourceClass, "The source class to convert from is required");
		Assert.notNull(targetClass, "The target class to convert to is required");
		if (targetClass.isAssignableFrom(sourceClass)) {
			return new StaticConversionExecutor(sourceClass, targetClass, new NoOpConverter());
		}
		sourceClass = convertToWrapperClassIfNecessary(sourceClass);
		targetClass = convertToWrapperClassIfNecessary(targetClass);
		// special handling for arrays since they are not indexable classes
		if (sourceClass.isArray()) {
			if (targetClass.isArray()) {
				return new StaticSuperConversionExecutor(sourceClass, targetClass, new ArrayToArray(this));
			} else if (Collection.class.isAssignableFrom(targetClass)) {
				if (!targetClass.isInterface() && Modifier.isAbstract(targetClass.getModifiers())) {
					throw new IllegalArgumentException("Conversion target class [" + targetClass.getName()
							+ "] is invalid; cannot convert to abstract collection types--"
							+ "request an interface or concrete implementation instead");
				}
				return new StaticSuperConversionExecutor(sourceClass, targetClass, new ArrayToCollection(this));
			}
		}
		if (targetClass.isArray()) {
			if (Collection.class.isAssignableFrom(sourceClass)) {
				return new StaticSuperConversionExecutor(sourceClass, targetClass, new CollectionToArray(this));
			} else {
				return new StaticSuperConversionExecutor(sourceClass, targetClass, new ObjectToArray(this));
			}
		}
		Converter converter = findRegisteredConverter(sourceClass, targetClass);
		if (converter != null) {
			// we found a converter
			return new StaticConversionExecutor(sourceClass, targetClass, converter);
		} else {
			if (parent != null) {
				// try the parent
				return parent.getConversionExecutor(sourceClass, targetClass);
			} else {
				throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
						"No ConversionExecutor found for converting from sourceClass [" + sourceClass.getName()
								+ "] to target class [" + targetClass.getName() + "]");
			}
		}
	}

	public ConversionExecutor getConversionExecutor(String converterId, Class sourceClass, Class targetClass)
			throws ConversionExecutorNotFoundException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	public Class getClassForAlias(String name) throws IllegalArgumentException {
		Class clazz = (Class) aliasMap.get(name);
		if (clazz != null) {
			return clazz;
		} else {
			if (parent != null) {
				return parent.getClassForAlias(name);
			} else {
				return null;
			}
		}
	}

	// internal helpers

	private List getTypeInfo(Converter converter) {
		List typeInfo = new ArrayList(2);
		Class classToIntrospect = converter.getClass();
		while (classToIntrospect != null) {
			Type[] genericInterfaces = converter.getClass().getGenericInterfaces();
			for (Type genericInterface : genericInterfaces) {
				if (genericInterface instanceof ParameterizedType) {
					ParameterizedType parameterizedInterface = (ParameterizedType) genericInterface;
					if (Converter.class.equals(parameterizedInterface.getRawType())) {
						Type s = parameterizedInterface.getActualTypeArguments()[0];
						Type t = parameterizedInterface.getActualTypeArguments()[1];
						typeInfo.add(getParameterClass(s, converter.getClass()));
						typeInfo.add(getParameterClass(t, converter.getClass()));
					}
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return typeInfo;
	}

	private Class<?> getParameterClass(Type parameterType, Class<?> converterClass) {
		if (parameterType instanceof TypeVariable) {
			parameterType = GenericTypeResolver.resolveTypeVariable((TypeVariable<?>) parameterType, converterClass);
		}
		if (parameterType instanceof Class) {
			return (Class<?>) parameterType;
		}
		// when would this happen?
		return null;
	}

	private Map getSourceMap(Class sourceClass) {
		Map sourceMap = (Map) sourceClassConverters.get(sourceClass);
		if (sourceMap == null) {
			sourceMap = new HashMap<Class<?>, Converter<?, ?>>();
			sourceClassConverters.put(sourceClass, sourceMap);
		}
		return sourceMap;
	}

	private Class convertToWrapperClassIfNecessary(Class targetType) {
		if (targetType.isPrimitive()) {
			if (targetType.equals(int.class)) {
				return Integer.class;
			} else if (targetType.equals(short.class)) {
				return Short.class;
			} else if (targetType.equals(long.class)) {
				return Long.class;
			} else if (targetType.equals(float.class)) {
				return Float.class;
			} else if (targetType.equals(double.class)) {
				return Double.class;
			} else if (targetType.equals(byte.class)) {
				return Byte.class;
			} else if (targetType.equals(boolean.class)) {
				return Boolean.class;
			} else if (targetType.equals(char.class)) {
				return Character.class;
			} else {
				throw new IllegalStateException("Should never happen - primitive type is not a primitive?");
			}
		} else {
			return targetType;
		}
	}

	private Converter findRegisteredConverter(Class sourceClass, Class targetClass) {
		if (sourceClass.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map<Class, Converter> sourceTargetConverters = findConvertersForSource(currentClass);
				Converter converter = findTargetConverter(sourceTargetConverters, targetClass);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			Map objectConverters = findConvertersForSource(Object.class);
			return findTargetConverter(objectConverters, targetClass);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map sourceTargetConverters = findConvertersForSource(currentClass);
				Converter converter = findTargetConverter(sourceTargetConverters, targetClass);
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

	private Map findConvertersForSource(Class sourceClass) {
		Map sourceConverters = (Map) sourceClassConverters.get(sourceClass);
		return sourceConverters != null ? sourceConverters : Collections.emptyMap();
	}

	private Converter findTargetConverter(Map sourceTargetConverters, Class targetClass) {
		if (sourceTargetConverters.isEmpty()) {
			return null;
		}
		return (Converter) sourceTargetConverters.get(targetClass);
	}
}