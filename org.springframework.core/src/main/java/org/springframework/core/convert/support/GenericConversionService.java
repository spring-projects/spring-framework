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

import static org.springframework.core.convert.support.ConversionUtils.invokeConverter;

import java.lang.reflect.Array;
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
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 * @see #addConverter(Converter)
 * @see #addConverterFactory(ConverterFactory)
 */
public class GenericConversionService implements ConversionService, ConverterRegistry {

	private static final GenericConverter NO_OP_CONVERTER = new GenericConverter() {
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return source;
		}
	};

	private final Map<Class, Map<Class, GenericConverter>> sourceTypeConverters = new HashMap<Class, Map<Class, GenericConverter>>();

	private ConversionService parent;

	private GenericConverter parentConverterAdapter = new GenericConverter() {
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return parent.convert(source, sourceType, targetType);
		}
	};

	public GenericConversionService() {
		initGenericConverters();
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
		getSourceMap(sourceType).remove(targetType);
	}

	// implementing ConversionService

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		return canConvert(TypeDescriptor.valueOf(sourceType), TypeDescriptor.valueOf(targetType));
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Class<T> targetType) {
		Assert.notNull(targetType, "The targetType to convert to is required");
		return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(sourceType, "The sourceType to convert from is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
		if (targetType == TypeDescriptor.NULL) {
			return true;
		}
		return getConverter(sourceType, targetType) != null;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(sourceType, "The sourceType to convert to is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
		if (source == null) {
			return convertNull(sourceType, targetType);
		}
		Assert.isTrue(sourceType != TypeDescriptor.NULL,
				"The source TypeDescriptor must not be TypeDescriptor.NULL when source != null");
		if (targetType == TypeDescriptor.NULL) {
			return null;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter == null) {
			throw new ConverterNotFoundException(sourceType, targetType);
		}
		return invokeConverter(converter, source, sourceType, targetType);
	}

	// subclassing hooks

	/**
	 * Hook to initialize the "generic" converters that require the full TypeDescriptor context to perform their conversion operations.
	 */
	protected void initGenericConverters() {
		addGenericConverter(Object[].class, Object[].class, new ArrayToArrayGenericConverter(this));
		addGenericConverter(Object[].class, Collection.class, new ArrayToCollectionGenericConverter(this));
		addGenericConverter(Object[].class, Map.class, new ArrayToMapGenericConverter(this));
		addGenericConverter(Object[].class, Object.class, new ArrayToObjectGenericConverter(this));
		addGenericConverter(Collection.class, Collection.class, new CollectionToCollectionGenericConverter(this));
		addGenericConverter(Collection.class, Object[].class, new CollectionToArrayGenericConverter(this));
		addGenericConverter(Collection.class, Map.class, new CollectionToMapGenericConverter(this));
		addGenericConverter(Collection.class, Object.class, new CollectionToObjectGenericConverter(this));
		addGenericConverter(Map.class, Map.class, new MapToMapGenericConverter(this));
		addGenericConverter(Map.class, Object[].class, new MapToArrayGenericConverter(this));
		addGenericConverter(Map.class, Collection.class, new MapToCollectionGenericConverter(this));
		addGenericConverter(Map.class, Object.class, new MapToObjectGenericConverter(this));
		addGenericConverter(Object.class, Object[].class, new ObjectToArrayGenericConverter(this));
		addGenericConverter(Object.class, Collection.class, new ObjectToCollectionGenericConverter(this));
		addGenericConverter(Object.class, Map.class, new ObjectToMapGenericConverter(this));
	}

	/**
	 * Registers a GenericConverter.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @param converter the generic converter.
	 */
	protected void addGenericConverter(Class<?> sourceType, Class<?> targetType, GenericConverter converter) {
		getSourceMap(sourceType).put(targetType, converter);
	}

	/**
	 * Hook method to convert a null value.
	 * Default implementation simply returns <code>null</code>.
	 * Subclasses may override to return a custom null objects for specific target types.
	 * @param sourceType the sourceType
	 * @param targetType the tagetType
	 * @return the null object
	 */
	protected Object convertNull(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return null;
	}

	/**
	 * Hook method to lookup the converter for a given sourceType/targetType pair.
	 * First queries this ConversionService's converter map.
	 * If no suitable Converter is found, and a {@link #setParent(GenericConversionService) parent} is set, then queries the parent.
	 * If still no suitable Converter is found, returns a NO_OP Converter if the sourceType and targetType are assignable.
	 * Returns <code>null</code> if this ConversionService simply cannot convert between sourceType and targetType.
	 * Subclasses may override.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the generic converter that will perform the conversion, or <code>null</code> if no suitable converter was found
	 */
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		GenericConverter converter = findConverterByClassPair(sourceType.getObjectType(), targetType.getObjectType());
		if (converter != null) {
			return converter;
		} else if (this.parent != null && this.parent.canConvert(sourceType, targetType)) {
			return this.parentConverterAdapter;
		} else {
			if (sourceType.isAssignableTo(targetType)) {
				return NO_OP_CONVERTER;
			} else {
				return null;
			}
		}
	}

	// internal helpers

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

	private GenericConverter findConverterByClassPair(Class sourceType, Class targetType) {
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
				if (currentClass.isArray()) {
					Class componentType = ClassUtils.resolvePrimitiveIfNecessary(currentClass.getComponentType());
					if (componentType.getSuperclass() != null) {
						classQueue.addFirst(Array.newInstance(componentType.getSuperclass(), 0).getClass());
					}
				} else {
					if (currentClass.getSuperclass() != null) {
						classQueue.addFirst(currentClass.getSuperclass());
					}
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (Class ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return null;
		}
	}

	private Map<Class, GenericConverter> getSourceMap(Class sourceType) {
		Map<Class, GenericConverter> sourceMap = sourceTypeConverters.get(sourceType);
		if (sourceMap == null) {
			sourceMap = new HashMap<Class, GenericConverter>();
			this.sourceTypeConverters.put(sourceType, sourceMap);
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

	private GenericConverter getConverter(Map<Class, GenericConverter> converters, Class targetType) {
		if (targetType.isInterface()) {
			LinkedList<Class> classQueue = new LinkedList<Class>();
			classQueue.addFirst(targetType);
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
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class currentClass = classQueue.removeLast();
				GenericConverter converter = converters.get(currentClass);
				if (converter != null) {
					return converter;
				}
				if (currentClass.isArray()) {
					Class componentType = ClassUtils.resolvePrimitiveIfNecessary(currentClass.getComponentType());
					if (componentType.getSuperclass() != null) {
						classQueue.addFirst(Array.newInstance(componentType.getSuperclass(), 0).getClass());
					}
				} else {
					if (currentClass.getSuperclass() != null) {
						classQueue.addFirst(currentClass.getSuperclass());
					}
				}
				Class[] interfaces = currentClass.getInterfaces();
				for (Class ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return null;
		}
	}

	private static class ConverterAdapter implements GenericConverter {

		private Converter converter;

		public ConverterAdapter(Converter converter) {
			this.converter = converter;
		}

		@SuppressWarnings("unchecked")
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return this.converter.convert(source);
		}
	}

	private static class ConverterFactoryAdapter implements GenericConverter {

		private ConverterFactory converterFactory;

		public ConverterFactoryAdapter(ConverterFactory converterFactory) {
			this.converterFactory = converterFactory;
		}

		@SuppressWarnings("unchecked")
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}
	}

}
