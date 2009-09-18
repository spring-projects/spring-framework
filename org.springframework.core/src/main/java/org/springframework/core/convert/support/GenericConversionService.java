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
import java.util.Set;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterInfo;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base implementation of a conversion service.
 * Initially empty, e.g. no converters are registered by default.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see #addConverter(Converter)
 * @see #addConverterFactory(ConverterFactory)
 */
public class GenericConversionService implements ConversionService, ConverterRegistry {

	private ConversionService parent;

	private final Map<Class, Map<Class, GenericConverter>> sourceTypeConverters = new HashMap<Class, Map<Class, GenericConverter>>();

	public GenericConversionService() {
		addGenericConverter(Collection.class, Collection.class, new CollectionToCollectionGenericConverter(this));
		addGenericConverter(Map.class, Map.class, new MapToMapGenericConverter(this));
	}

	/**
	 * Registers the converters in the set provided.
	 * JavaBean-friendly alternative to calling {@link #addConverter(Converter)}.
	 * @see #addConverter(Converter)
	 */
	public void setConverters(Set<Converter> converters) {
		for (Converter converter : converters) {
			addConverter(converter);
		}
	}

	/**
	 * Registers the converter factories in the set provided.
	 * JavaBean-friendly alternative to calling {@link #addConverterFactory(ConverterFactory)}.
	 * @see #addConverterFactory(ConverterFactory)
	 */
	public void setConverterFactories(Set<ConverterFactory> converters) {
		for (ConverterFactory converterFactory : converters) {
			addConverterFactory(converterFactory);
		}
	}

	/**
	 * Set the parent of this conversion service. This is optional.
	 */
	public void setParent(ConversionService parent) {
		this.parent = parent;
	}

	/**
	 * Returns the parent of this conversion service. Could be null.
	 */
	public ConversionService getParent() {
		return this.parent;
	}

	// implementing ConverterRegistry

	public void addConverter(Converter converter) {
		List<Class> typeInfo = getRequiredTypeInfo(converter);
		if (typeInfo == null) {
			throw new IllegalArgumentException(
					"Unable to the determine sourceType <S> and targetType <T> your Converter<S, T> converts between; declare these types or implement ConverterInfo");
		}
		Class sourceType = typeInfo.get(0);
		Class targetType = typeInfo.get(1);
		getSourceMap(sourceType).put(targetType, new ConverterAdapter(converter));
	}

	public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
		List<Class> typeInfo = getRequiredTypeInfo(converterFactory);
		if (typeInfo == null) {
			throw new IllegalArgumentException(
					"Unable to the determine sourceType <S> and targetRangeType R your ConverterFactory<S, R> converts between; declare these types or implement ConverterInfo");
		}
		Class sourceType = typeInfo.get(0);
		Class targetType = typeInfo.get(1);
		getSourceMap(sourceType).put(targetType, new ConverterFactoryAdapter(converterFactory));
	}

	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		Map sourceMap = getSourceMap(sourceType);
		sourceMap.remove(targetType);
	}

	// implementing ConversionService

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(sourceType, TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(Class<?> sourceType, TypeDescriptor targetType) {
		Assert.notNull(sourceType, "The sourceType to convert from is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
		if (targetType == TypeDescriptor.NULL) {
			return true;
		}
		Class sourceClass = ClassUtils.resolvePrimitiveIfNecessary(sourceType);
		Class targetClass = targetType.getObjectType();
		return getConverter(sourceClass, targetType) != null || this.parent != null
				&& this.parent.canConvert(sourceClass, targetClass);
	}

	public <T> T convert(Object source, Class<T> targetType) {
		Assert.notNull(targetType, "The targetType to convert to is required");
		return (T) convert(source, TypeDescriptor.valueOf(targetType));
	}

	public Object convert(Object source, TypeDescriptor targetType) {
		Assert.notNull(targetType, "The targetType to convert to is required");
		if (source == null) {
			return null;
		}
		if (targetType == TypeDescriptor.NULL) {
			return null;
		}
		Class sourceType = ClassUtils.resolvePrimitiveIfNecessary(source.getClass());
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter != null) {
			try {
				return converter.convert(source, targetType);
			} catch (ConversionFailedException e) {
				throw e;
			} catch (Exception e) {
				throw new ConversionFailedException(sourceType, targetType, source, e);
			}
		} else {
			if (targetType.isAssignableValue(source)) {
				return source;
			} else {
				throw new ConverterNotFoundException(sourceType, targetType.getObjectType());
			}
		}
	}

	// subclassing hooks

	protected GenericConverter getConverter(Class sourceType, TypeDescriptor targetType) {
		if (sourceType.isInterface()) {
			LinkedList<Class> classQueue = new LinkedList<Class>();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class currentClass = classQueue.removeLast();
				Map<Class, GenericConverter> converters = getConvertersForSource(currentClass);
				GenericConverter converter = getConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (Class ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			Map<Class, GenericConverter> objectConverters = getConvertersForSource(Object.class);
			return getConverter(objectConverters, targetType);
		} else {
			LinkedList<Class> classQueue = new LinkedList<Class>();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class currentClass = classQueue.removeLast();
				Map<Class, GenericConverter> converters = getConvertersForSource(currentClass);
				GenericConverter converter = getConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				if (currentClass.getSuperclass() != null) {
					classQueue.addFirst(currentClass.getSuperclass());
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (Class ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return null;
		}
	}

	// internal helpers

	private void addGenericConverter(Class sourceType, Class targetType, GenericConverter converter) {
		getSourceMap(sourceType).put(targetType, converter);
	}

	private List<Class> getRequiredTypeInfo(Object converter) {
		List<Class> typeInfo = new ArrayList<Class>(2);
		if (converter instanceof ConverterInfo) {
			ConverterInfo info = (ConverterInfo) converter;
			typeInfo.add(info.getSourceType());
			typeInfo.add(info.getTargetType());
			return typeInfo;
		} else {
			return getConverterTypeInfo(converter.getClass());
		}
	}

	private List<Class> getConverterTypeInfo(Class converterClass) {
		Class classToIntrospect = converterClass;
		while (classToIntrospect != null) {
			Type[] ifcs = classToIntrospect.getGenericInterfaces();
			for (Type ifc : ifcs) {
				if (ifc instanceof ParameterizedType) {
					ParameterizedType paramIfc = (ParameterizedType) ifc;
					Type rawType = paramIfc.getRawType();
					if (Converter.class.equals(rawType) || ConverterFactory.class.equals(rawType)) {
						List<Class> typeInfo = new ArrayList<Class>(2);
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
					} else if (Converter.class.isAssignableFrom((Class) rawType)) {
						return getConverterTypeInfo((Class) rawType);
					}
				} else if (Converter.class.isAssignableFrom((Class) ifc)) {
					return getConverterTypeInfo((Class) ifc);
				}
			}
			classToIntrospect = classToIntrospect.getSuperclass();
		}
		return null;
	}

	private Map<Class, GenericConverter> getSourceMap(Class sourceType) {
		Map<Class, GenericConverter> sourceMap = sourceTypeConverters.get(sourceType);
		if (sourceMap == null) {
			sourceMap = new HashMap<Class, GenericConverter>();
			sourceTypeConverters.put(sourceType, sourceMap);
		}
		return sourceMap;
	}

	private Map<Class, GenericConverter> getConvertersForSource(Class sourceType) {
		Map<Class, GenericConverter> converters = this.sourceTypeConverters.get(sourceType);
		if (converters == null) {
			converters = Collections.emptyMap();
		}
		return converters;
	}

	private GenericConverter getConverter(Map<Class, GenericConverter> converters, TypeDescriptor targetType) {
		Class targetClass = targetType.getObjectType();
		if (targetClass.isInterface()) {
			LinkedList<Class> classQueue = new LinkedList<Class>();
			classQueue.addFirst(targetClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = classQueue.removeLast();
				GenericConverter converter = converters.get(currentClass);
				if (converter != null) {
					return converter;
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (Class ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return converters.get(Object.class);
		} else {
			LinkedList<Class> classQueue = new LinkedList<Class>();
			classQueue.addFirst(targetClass);
			while (!classQueue.isEmpty()) {
				Class currentClass = classQueue.removeLast();
				GenericConverter converter = converters.get(currentClass);
				if (converter != null) {
					return converter;
				}
				if (currentClass.getSuperclass() != null) {
					classQueue.addFirst(currentClass.getSuperclass());
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (Class ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return null;
		}
	}

	static class ConverterAdapter implements GenericConverter {

		private Converter converter;

		public ConverterAdapter(Converter converter) {
			this.converter = converter;
		}

		public Object convert(Object source, TypeDescriptor type) {
			return converter.convert(source);
		}

	}

	static class ConverterFactoryAdapter implements GenericConverter {

		private ConverterFactory converterFactory;

		public ConverterFactoryAdapter(ConverterFactory converterFactory) {
			this.converterFactory = converterFactory;
		}

		public Object convert(Object source, TypeDescriptor targetType) {
			return converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}

	}

}
