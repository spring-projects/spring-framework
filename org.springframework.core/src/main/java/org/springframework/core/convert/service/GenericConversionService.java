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
import org.springframework.core.convert.converter.SuperTwoWayConverter;
import org.springframework.util.Assert;

/**
 * Base implementation of a conversion service. Initially empty, e.g. no converters are registered by default.
 * 
 * TODO auto-conversion of generic collection elements
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
	 * An indexed map of SuperConverters. Each Map.Entry key is a source class (S) that can be converted from. Each
	 * Map.Entry value is a Map that defines the targetClass-to-SuperConverter mappings for that source.
	 */
	private final Map sourceClassSuperConverters = new HashMap();

	/**
	 * A map of custom converters. Custom converters are assigned a unique identifier that can be used to lookup the
	 * converter. This allows multiple converters for the same source->target class to be registered.
	 */
	private final Map customConverters = new HashMap();

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
		List typeInfo = getRequiredTypeInfo(converter);
		Class sourceClass = (Class) typeInfo.get(0);
		Class targetClass = (Class) typeInfo.get(1);
		// index forward
		Map sourceMap = getSourceSuperConverterMap(sourceClass);
		sourceMap.put(targetClass, converter);
		if (converter instanceof SuperTwoWayConverter) {
			// index reverse
			sourceMap = getSourceSuperConverterMap(targetClass);
			sourceMap.put(sourceClass, new ReverseSuperConverter((SuperTwoWayConverter) converter));
		}
	}

	/**
	 * Register the converter as a custom converter with this conversion service.
	 * @param id the id to assign the converter
	 * @param converter the converter to use a custom converter
	 */
	public void addConverter(String id, Converter converter) {
		customConverters.put(id, converter);
	}

	/**
	 * Adapts a {@link SuperTwoWayConverter} that converts between BS and BT class hierarchies to a {@link Converter}
	 * that converts between the specific BS/BT sub types S and T.
	 * 
	 * TODO - I think this is going to force indexing on a getSourceClass/getTargetclass prop instead generic args
	 * 
	 * @param sourceClass the source class S to convert from, which must be equal or extend BS
	 * @param targetClass the target type T to convert to, which must equal or extend BT
	 * @param converter the super two way converter
	 * @return a converter that converts from S to T by delegating to the super converter
	 */
	public static <S, T> Converter<S, T> converterFor(Class<S> sourceClass, Class<T> targetClass,
			SuperTwoWayConverter converter) {
		return new SuperTwoWayConverterConverter<S, T>(converter, sourceClass, targetClass);
	}

	/**
	 * Add a convenient alias for the target type. {@link #getClassForAlias(String)} can then be used to lookup the type
	 * given the alias.
	 * @see #getClassForAlias(String)
	 */
	public void addAlias(String alias, Class targetType) {
		aliasMap.put(alias, targetType);
	}

	// implementing ConversionService

	public Object executeConversion(Object source, Class targetClass) throws ConversionExecutorNotFoundException,
			ConversionException {
		return getConversionExecutor(source.getClass(), targetClass).execute(source);
	}

	public Object executeConversion(String converterId, Object source, Class targetClass)
			throws ConversionExecutorNotFoundException, ConversionException {
		return getConversionExecutor(converterId, source.getClass(), targetClass).execute(source);
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
				SuperConverter collectionToArray = new ReverseSuperConverter(new ArrayToCollection(this));
				return new StaticSuperConversionExecutor(sourceClass, targetClass, collectionToArray);
			} else {
				return new StaticSuperConversionExecutor(sourceClass, targetClass, new ObjectToArray(this));
			}
		}
		Converter converter = findRegisteredConverter(sourceClass, targetClass);
		if (converter != null) {
			// we found a converter
			return new StaticConversionExecutor(sourceClass, targetClass, converter);
		} else {
			SuperConverter superConverter = findRegisteredSuperConverter(sourceClass, targetClass);
			if (superConverter != null) {
				return new StaticSuperConversionExecutor(sourceClass, targetClass, superConverter);
			}
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

	public ConversionExecutor getConversionExecutor(String id, Class sourceClass, Class targetClass)
			throws ConversionExecutorNotFoundException {
		Assert.hasText(id, "The id of the custom converter is required");
		Assert.notNull(sourceClass, "The source class to convert from is required");
		Assert.notNull(targetClass, "The target class to convert to is required");
		Converter converter = (Converter) customConverters.get(id);
		if (converter == null) {
			if (parent != null) {
				return parent.getConversionExecutor(id, sourceClass, targetClass);
			} else {
				throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
						"No custom Converter found with id '" + id + "' for converting from sourceClass ["
								+ sourceClass.getName() + "] to targetClass [" + targetClass.getName() + "]");
			}
		}
		sourceClass = convertToWrapperClassIfNecessary(sourceClass);
		targetClass = convertToWrapperClassIfNecessary(targetClass);
		// TODO Not optimal getting this each time
		List typeInfo = getRequiredTypeInfo(converter);
		Class converterSourceClass = (Class) typeInfo.get(0);
		Class converterTargetClass = (Class) typeInfo.get(1);
		if (sourceClass.isArray()) {
			Class sourceComponentType = sourceClass.getComponentType();
			if (targetClass.isArray()) {
				Class targetComponentType = targetClass.getComponentType();
				if (converterSourceClass.isAssignableFrom(sourceComponentType)) {
					if (!converterTargetClass.equals(targetComponentType)) {
						throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
								"Custom Converter with id '" + id
										+ "' cannot convert from an array storing elements of type ["
										+ sourceComponentType.getName() + "] to an array of storing elements of type ["
										+ targetComponentType.getName() + "]");
					}
					ConversionExecutor elementConverter = new StaticConversionExecutor(sourceComponentType,
							targetComponentType, converter);
					return new StaticSuperConversionExecutor(sourceClass, targetClass, new ArrayToArray(
							elementConverter));
				} else if (converterTargetClass.isAssignableFrom(sourceComponentType)) {
					if (!converterSourceClass.equals(targetComponentType)) {
						throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
								"Custom Converter with id '" + id
										+ "' cannot convert from an array storing elements of type ["
										+ sourceComponentType.getName() + "] to an array of storing elements of type ["
										+ targetComponentType.getName() + "]");
					}
					ConversionExecutor elementConverter = new StaticConversionExecutor(sourceComponentType,
							targetComponentType, new ReverseConverter(converter));
					return new StaticSuperConversionExecutor(sourceClass, targetClass, new ArrayToArray(
							elementConverter));
				} else {
					throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
							"Custom Converter with id '" + id
									+ "' cannot convert from an array storing elements of type ["
									+ sourceComponentType.getName() + "] to an array storing elements of type ["
									+ targetComponentType.getName() + "]");
				}
			} else if (Collection.class.isAssignableFrom(targetClass)) {
				if (!targetClass.isInterface() && Modifier.isAbstract(targetClass.getModifiers())) {
					throw new IllegalArgumentException("Conversion target class [" + targetClass.getName()
							+ "] is invalid; cannot convert to abstract collection types--"
							+ "request an interface or concrete implementation instead");
				}
				if (converterSourceClass.isAssignableFrom(sourceComponentType)) {
					// type erasure has prevented us from getting the concrete type, this is best we can do for now
					ConversionExecutor elementConverter = new StaticConversionExecutor(sourceComponentType,
							converterTargetClass, converter);
					return new StaticSuperConversionExecutor(sourceClass, targetClass, new ArrayToCollection(
							elementConverter));
				} else if (converterTargetClass.isAssignableFrom(sourceComponentType)) {
					ConversionExecutor elementConverter = new StaticConversionExecutor(sourceComponentType,
							converterSourceClass, new ReverseConverter(converter));
					return new StaticSuperConversionExecutor(sourceClass, targetClass, new ArrayToCollection(
							elementConverter));
				} else {
					throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
							"Custom Converter with id '" + id
									+ "' cannot convert from array an storing elements type ["
									+ sourceComponentType.getName() + "] to a collection of type ["
									+ targetClass.getName() + "]");
				}
			}
		}
		if (targetClass.isArray()) {
			Class targetComponentType = targetClass.getComponentType();
			if (Collection.class.isAssignableFrom(sourceClass)) {
				// type erasure limits us here as well
				if (converterTargetClass.equals(targetComponentType)) {
					ConversionExecutor elementConverter = new StaticConversionExecutor(converterSourceClass,
							targetComponentType, converter);
					SuperConverter collectionToArray = new ReverseSuperConverter(
							new ArrayToCollection(elementConverter));
					return new StaticSuperConversionExecutor(sourceClass, targetClass, collectionToArray);
				} else if (converterSourceClass.equals(targetComponentType)) {
					ConversionExecutor elementConverter = new StaticConversionExecutor(converterTargetClass,
							targetComponentType, new ReverseConverter(converter));
					SuperConverter collectionToArray = new ReverseSuperConverter(
							new ArrayToCollection(elementConverter));
					return new StaticSuperConversionExecutor(sourceClass, targetClass, collectionToArray);
				} else {
					throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
							"Custom Converter with id '" + id + "' cannot convert from collection of type ["
									+ sourceClass.getName() + "] to an array storing elements of type ["
									+ targetComponentType.getName() + "]");
				}
			} else {
				if (converterSourceClass.isAssignableFrom(sourceClass)) {
					if (!converterTargetClass.equals(targetComponentType)) {
						throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
								"Custom Converter with id '" + id + "' cannot convert from sourceClass ["
										+ sourceClass.getName() + "] to array holding elements of type ["
										+ targetComponentType.getName() + "]");
					}
					ConversionExecutor elementConverter = new StaticConversionExecutor(sourceClass,
							targetComponentType, converter);
					return new StaticSuperConversionExecutor(sourceClass, targetClass, new ObjectToArray(
							elementConverter));
				} else if (converterTargetClass.isAssignableFrom(sourceClass)) {
					if (!converterSourceClass.equals(targetComponentType)) {
						throw new ConversionExecutorNotFoundException(sourceClass, targetClass,
								"Custom Converter with id '" + id + "' cannot convert from sourceClass ["
										+ sourceClass.getName() + "] to array holding elements of type ["
										+ targetComponentType.getName() + "]");
					}
					ConversionExecutor elementConverter = new StaticConversionExecutor(sourceClass,
							targetComponentType, new ReverseConverter(converter));
					return new StaticSuperConversionExecutor(sourceClass, targetClass, new ObjectToArray(
							elementConverter));
				}
			}
		}
		// TODO look to factor some of this duplicated code here and above out a bit
		if (converterSourceClass.isAssignableFrom(sourceClass)) {
			if (!converterTargetClass.equals(targetClass)) {
				throw new ConversionExecutorNotFoundException(sourceClass, targetClass, "Custom Converter with id '"
						+ id + "' cannot convert from sourceClass [" + sourceClass.getName() + "] to targetClass ["
						+ targetClass.getName() + "]");
			}
			return new StaticConversionExecutor(sourceClass, targetClass, converter);
		} else if (converterTargetClass.isAssignableFrom(sourceClass)) {
			if (!converterSourceClass.equals(targetClass)) {
				throw new ConversionExecutorNotFoundException(sourceClass, targetClass, "Custom Converter with id '"
						+ id + "' cannot convert from sourceClass [" + sourceClass.getName() + "] to targetClass ["
						+ targetClass.getName() + "]");
			}
			return new StaticConversionExecutor(sourceClass, targetClass, new ReverseConverter(converter));
		} else {
			throw new ConversionExecutorNotFoundException(sourceClass, targetClass, "Custom Converter with id '" + id
					+ "' cannot convert from sourceClass [" + sourceClass.getName() + "] to targetClass ["
					+ targetClass.getName() + "]");
		}
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

	private List getRequiredTypeInfo(Object converter) {
		List typeInfo = new ArrayList(2);
		Class classToIntrospect = converter.getClass();
		while (classToIntrospect != null) {
			Type[] genericInterfaces = classToIntrospect.getGenericInterfaces();
			for (Type genericInterface : genericInterfaces) {
				if (genericInterface instanceof ParameterizedType) {
					ParameterizedType parameterizedInterface = (ParameterizedType) genericInterface;
					if (Converter.class.equals(parameterizedInterface.getRawType())
							|| SuperConverter.class.isAssignableFrom((Class) parameterizedInterface.getRawType())) {
						Class s = getParameterClass(parameterizedInterface.getActualTypeArguments()[0], converter
								.getClass());
						Class t = getParameterClass(parameterizedInterface.getActualTypeArguments()[1], converter
								.getClass());
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

	private Map getSourceMap(Class sourceClass) {
		Map sourceMap = (Map) sourceClassConverters.get(sourceClass);
		if (sourceMap == null) {
			sourceMap = new HashMap();
			sourceClassConverters.put(sourceClass, sourceMap);
		}
		return sourceMap;
	}

	private Map getSourceSuperConverterMap(Class sourceClass) {
		Map sourceMap = (Map) sourceClassSuperConverters.get(sourceClass);
		if (sourceMap == null) {
			sourceMap = new HashMap();
			sourceClassSuperConverters.put(sourceClass, sourceMap);
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
				Map converters = getConvertersForSource(currentClass);
				Converter converter = getConverter(converters, targetClass);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			Map objectConverters = getConvertersForSource(Object.class);
			return getConverter(objectConverters, targetClass);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getConvertersForSource(currentClass);
				Converter converter = getConverter(converters, targetClass);
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

	private Map getConvertersForSource(Class sourceClass) {
		Map converters = (Map) sourceClassConverters.get(sourceClass);
		return converters != null ? converters : Collections.emptyMap();
	}

	private Converter getConverter(Map converters, Class targetClass) {
		return (Converter) converters.get(targetClass);
	}

	private SuperConverter findRegisteredSuperConverter(Class sourceClass, Class targetClass) {
		if (sourceClass.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getSuperConvertersForSource(currentClass);
				SuperConverter converter = findSuperConverter(converters, targetClass);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			Map objectConverters = getSuperConvertersForSource(Object.class);
			return findSuperConverter(objectConverters, targetClass);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(sourceClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Map converters = getSuperConvertersForSource(currentClass);
				SuperConverter converter = findSuperConverter(converters, targetClass);
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

	private Map getSuperConvertersForSource(Class sourceClass) {
		Map converters = (Map) sourceClassSuperConverters.get(sourceClass);
		return converters != null ? converters : Collections.emptyMap();
	}

	private SuperConverter findSuperConverter(Map converters, Class targetClass) {
		if (converters.isEmpty()) {
			return null;
		}
		if (targetClass.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(targetClass);
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
			classQueue.addFirst(targetClass);
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