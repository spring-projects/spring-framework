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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterInfo;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.Assert;

/**
 * Base implementation of a conversion service.
 * Initially empty, e.g. no converters are registered by default.
 * @author Keith Donald
 * @see #add(Converter)
 * @see #add(ConverterFactory)
 * @since 3.0
 */
@SuppressWarnings("unchecked")
public class GenericTypeConverter implements TypeConverter, ConverterRegistry {

	/**
	 * An indexed map of Converters. Each Map.Entry key is a source class (S) that can be converted from. Each Map.Entry
	 * value is a Map that defines the targetType-to-Converter mappings for that source.
	 */
	private final Map sourceTypeConverters = new HashMap();

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

	// implementing ConverterRegistry
	
	public void add(Converter converter) {
		List typeInfo = getRequiredTypeInfo(converter);
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to the determine sourceType <S> and targetType <T> your Converter<S, T> converts between");
		}
		Class sourceType = (Class) typeInfo.get(0);
		Class targetType = (Class) typeInfo.get(1);
		Map sourceMap = getSourceMap(sourceType);
		sourceMap.put(targetType, converter);
	}

	public void add(ConverterFactory<?, ?> converterFactory) {
		List typeInfo = getRequiredTypeInfo(converterFactory);
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to the determine sourceType <S> and targetType <T> your ConverterFactory<S, T> creates Converters to convert between");
		}
		Class sourceType = (Class) typeInfo.get(0);
		Class targetType = (Class) typeInfo.get(1);
		Map sourceMap = getSourceMap(sourceType);
		sourceMap.put(targetType, converterFactory);
	}

	public void removeConverter(Class<?> sourceType, Class<?> targetType) {
		Map sourceMap = getSourceMap(sourceType);
		sourceMap.remove(targetType);
	}

	public void removeConverterFactory(ConverterFactory<?, ?> converter) {
		List typeInfo = getRequiredTypeInfo(converter);
		Class sourceType = (Class) typeInfo.get(0);
		Class targetType = (Class) typeInfo.get(1);
		Map sourceMap = getSourceMap(sourceType);
		ConverterFactory existing = (ConverterFactory) sourceMap.get(targetType);
		if (converter == existing) {
			sourceMap.remove(targetType);
		}
	}

	// implementing TypeConverter

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(sourceType, TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, TypeDescriptor<?> targetType) {
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
		return convert(source, TypeDescriptor.valueOf(targetType));
	}

	public <S, T> T convert(S source, TypeDescriptor<T> targetType) {
		if (source == null) {
			return null;
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

	ConversionExecutor getConversionExecutor(Class sourceClass, TypeDescriptor targetType)
			throws ConverterNotFoundException {
		Assert.notNull(sourceClass, "The sourceType to convert from is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
		if (targetType.getType() == null) {
			return NoOpConversionExecutor.INSTANCE;
		}
		// TODO clean this if/else code up
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
			} else if (targetType.isMap()) {
				if (sourceType.getElementType().equals(String.class)) {
					return new StringArrayToMap(sourceType, targetType, this);
				} else {
					// array to map
					return null;
				}
			} else {
				if (targetType.equals(String.class)) {
					// array to string;
					return null;
				} else {
					// array to object
					return null;
				}
			}
		}
		if (sourceType.isCollection()) {
			if (targetType.isCollection()) {	
				return new CollectionToCollection(sourceType, targetType, this);
			} else if (targetType.isArray()) {
				return new CollectionToArray(sourceType, targetType, this);
			} else if (targetType.isMap()) {
				if (sourceType.getElementType().equals(String.class)) {
					return new StringCollectionToMap(sourceType, targetType, this);
				} else {
					// object collection to map
					return null;
				}
			} else {
				if (targetType.equals(String.class)) {
					// collection to string;
					return null;
				} else {
					// collection to object
					return null;
				}
			}
		}
		if (sourceType.isMap()) {
			if (targetType.isMap()) {
				return new MapToMap(sourceType, targetType, this);
			} else if (targetType.isArray()) {
				if (targetType.getElementType().equals(String.class)) {
					return new MapToStringArray(sourceType, targetType, this);
				} else {
					// map to object array
					return null;
				}				
			} else if (targetType.isCollection()) {
				if (targetType.getElementType().equals(String.class)) {
					return new MapToStringCollection(sourceType, targetType, this);
				} else {
					// map to object collection
					return null;
				}				
			} else {
				// map to object
				return null;
			}
		}
		if (targetType.isArray()) {
			if (sourceType.getType().equals(String.class)) {
				return new StringToArray(sourceType, targetType, this);
			} else {
				return new ObjectToArray(sourceType, targetType, this);
			}
		}
		if (targetType.isCollection()) {
			if (sourceType.getType().equals(String.class)) {
				return new StringToCollection(sourceType, targetType, this);
			} else {
				return new ObjectToCollection(sourceType, targetType, this);
			}
		}
		if (targetType.isMap()) {
			if (sourceType.getType().equals(String.class)) {
				return new StringToMap(sourceType, targetType, this);
			} else {
				// object to map
				return null;
			}
		}
		if (sourceType.isAssignableTo(targetType)) {
			return NoOpConversionExecutor.INSTANCE;
		}
		Converter converter = findRegisteredConverter(sourceType.getType(), targetType.getType());
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
		} else {
			return getConverterTypeInfo(converter.getClass());
		}
	}

	private List getConverterTypeInfo(Class converterClass) {
		Class classToIntrospect = converterClass;
		while (classToIntrospect != null) {
			Type[] ifcs = classToIntrospect.getGenericInterfaces();
			for (Type ifc : ifcs) {
				if (ifc instanceof ParameterizedType) {
					ParameterizedType paramIfc = (ParameterizedType) ifc;
					Type rawType = paramIfc.getRawType();
					if (Converter.class.equals(rawType) || ConverterFactory.class.equals(rawType)) {
						List typeInfo = new ArrayList(2);						
						Type arg1 = paramIfc.getActualTypeArguments()[0];
						if (arg1 instanceof TypeVariable) {
							arg1 = GenericTypeResolver.resolveTypeVariable((TypeVariable) arg1, converterClass);
						}
						if (arg1 instanceof Class) {
							typeInfo.add((Class) arg1);
						}						
						Type arg2 = paramIfc.getActualTypeArguments()[1];
						if (arg2 instanceof TypeVariable) {
							arg2 = GenericTypeResolver.resolveTypeVariable((TypeVariable) arg2, converterClass);
						}
						if (arg2 instanceof Class) {
							typeInfo.add((Class) arg2);
						}
						if (typeInfo.size() == 2) {
							return typeInfo;
						}						
					}
					else if (Converter.class.isAssignableFrom((Class) rawType)) {
						return getConverterTypeInfo((Class) rawType);
					}
				}
				else if (Converter.class.isAssignableFrom((Class) ifc)) {
					return getConverterTypeInfo((Class) ifc);
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return null;
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
				System.out.println("Source:" + currentClass);
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
		if (targetType.isInterface()) {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Converter converter = getConverterImpl(converters, currentClass, targetType);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (int i = 0; i < interfaces.length; i++) {
					classQueue.addFirst(interfaces[i]);
				}
			}
			return getConverterImpl(converters, Object.class, targetType);
		} else {
			LinkedList classQueue = new LinkedList();
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class currentClass = (Class) classQueue.removeLast();
				Converter converter = getConverterImpl(converters, currentClass, targetType);
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
	
	private Converter getConverterImpl(Map converters, Class currentClass, Class targetType) {
		Object converter = converters.get(currentClass);
		if (converter == null) {
			return null;
		}
		if (converter instanceof Converter) {
			return (Converter) converter;
		} else {
			return ((ConverterFactory) converter).getConverter(targetType);
		}
	}

}