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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base ConversionService implementation suitable for use in most environments.
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class GenericConversionService implements ConversionService, ConverterRegistry {

	private static final GenericConverter NO_OP_CONVERTER = new GenericConverter() {
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return source;
		}
	};

	private final Map<Class<?>, Map<Class<?>, GenericConverter>> sourceTypeConverters = new HashMap<Class<?>, Map<Class<?>, GenericConverter>>(36);

	private ConversionService parent;

	private GenericConverter parentConverterAdapter = new GenericConverter() {
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return parent.convert(source, sourceType, targetType);
		}
	};

	/**
	 * Create a new GenericConversionService.
	 * Generic converters for Collection types are registered.
	 */
	public GenericConversionService() {
		addGenericConverter(Object[].class, Object[].class, new ArrayToArrayConverter(this));
		addGenericConverter(Object[].class, Collection.class, new ArrayToCollectionConverter(this));
		addGenericConverter(Object[].class, Map.class, new ArrayToMapConverter(this));
		addGenericConverter(Object[].class, Object.class, new ArrayToObjectConverter(this));
		addGenericConverter(Collection.class, Collection.class, new CollectionToCollectionConverter(this));
		addGenericConverter(Collection.class, Object[].class, new CollectionToArrayConverter(this));
		addGenericConverter(Collection.class, Map.class, new CollectionToMapConverter(this));
		addGenericConverter(Collection.class, Object.class, new CollectionToObjectConverter(this));
		addGenericConverter(Map.class, Map.class, new MapToMapConverter(this));
		addGenericConverter(Map.class, Object[].class, new MapToArrayConverter(this));
		addGenericConverter(Map.class, Collection.class, new MapToCollectionConverter(this));
		addGenericConverter(Map.class, Object.class, new MapToObjectConverter(this));
		addGenericConverter(Object.class, Object[].class, new ObjectToArrayConverter(this));
		addGenericConverter(Object.class, Collection.class, new ObjectToCollectionConverter(this));
		addGenericConverter(Object.class, Map.class, new ObjectToMapConverter(this));
	}

	/**
	 * Registers the converters in the set provided.
	 * JavaBean-friendly alternative to calling {@link #addConverter(Converter)}.
	 * @see #addConverter(Converter)
	 */
	public void setConverters(Set<Converter<?, ?>> converters) {
		for (Converter<?, ?> converter : converters) {
			addConverter(converter);
		}
	}

	/**
	 * Registers the converter factories in the set provided.
	 * JavaBean-friendly alternative to calling {@link #addConverterFactory(ConverterFactory)}.
	 * @see #addConverterFactory(ConverterFactory)
	 */
	public void setConverterFactories(Set<ConverterFactory<?, ?>> converters) {
		for (ConverterFactory<?, ?> converterFactory : converters) {
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
	 * Returns the parent of this conversion service. May be null.
	 */
	public ConversionService getParent() {
		return this.parent;
	}

	// implementing ConverterRegistry

	public void addConverter(Converter<?, ?> converter) {
		Class<?>[] typeInfo = getRequiredTypeInfo(converter, Converter.class);
		if (typeInfo == null) {
			throw new IllegalArgumentException(
					"Unable to the determine sourceType <S> and targetType <T> your Converter<S, T> converts between; declare these types or implement ConverterInfo");
		}
		Class<?> sourceType = typeInfo[0];
		Class<?> targetType = typeInfo[1];
		addConverter(sourceType, targetType, converter);
	}

	public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
		Class<?>[] typeInfo = getRequiredTypeInfo(converterFactory, ConverterFactory.class);
		if (typeInfo == null) {
			throw new IllegalArgumentException(
					"Unable to the determine sourceType <S> and targetRangeType R your ConverterFactory<S, R> converts between; declare these types or implement ConverterInfo");
		}
		Class<?> sourceType = typeInfo[0];
		Class<?> targetType = typeInfo[1];
		addConverterFactory(sourceType, targetType, converterFactory);
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
		return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		assertNotNull(sourceType, targetType);
		if (sourceType == TypeDescriptor.NULL || targetType == TypeDescriptor.NULL) {
			return true;
		}
		return getConverter(sourceType, targetType) != null;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		assertNotNull(sourceType, targetType);
		if (sourceType == TypeDescriptor.NULL) {
			Assert.isTrue(source == null, "The source must be null if sourceType == TypeDescriptor.NULL");
			return convertNullSource(sourceType, targetType);
		}
		if (targetType == TypeDescriptor.NULL) {
			return null;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter == null) {
			throw new ConverterNotFoundException(sourceType, targetType);
		}
		return invokeConverter(converter, source, sourceType, targetType);
	}

	/**
	 * Registers a GenericConverter.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @param converter the generic converter.
	 */
	public void addGenericConverter(Class<?> sourceType, Class<?> targetType, GenericConverter converter) {
		getSourceMap(sourceType).put(targetType, converter);
	}

	/**
	 * Registers a Converter with the sourceType and targetType to index on specified explicitly.
	 * This method performs better than {@link #addConverter(Converter)} because there parameterized types S and T don't have to be discovered.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @param converter the converter.
	 */
	public void addConverter(Class<?> sourceType, Class<?> targetType, Converter<?, ?> converter) {
		addGenericConverter(sourceType, targetType, new ConverterAdapter(converter));
	}

	/**
	 * Registers a ConverterFactory with the sourceType and targetType to index on specified explicitly.
	 * This method performs better than {@link #addConverter(ConverterFactory)} because there parameterized types S and T don't have to be discovered.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @param converter the converter.factory
	 */
	public void addConverterFactory(Class<?> sourceType, Class<?> targetType, ConverterFactory<?, ?> converterFactory) {
		addGenericConverter(sourceType, targetType, new ConverterFactoryAdapter(converterFactory));
	}

	// subclassing hooks

	/**
	 * Hook method to convert a null source.
	 * Default implementation returns <code>null</code>.
	 * Throws a {@link ConversionFailedException} if the targetType is a primitive type, as null cannot be assigned to a primitive type. 
	 * Subclasses may override to return custom null objects for specific target types.
	 * @param sourceType the sourceType to convert from
	 * @param targetType the targetType to convert to
	 * @return the converted null object
	 */
	protected Object convertNullSource(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.isPrimitive()) {
			throw new ConversionFailedException(sourceType, targetType, null, new IllegalArgumentException(
					"A null value cannot be assigned to a primitive type"));
		}
		return null;
	}

	/**
	 * Hook method to lookup the converter for a given sourceType/targetType pair.
	 * First queries this ConversionService's converter map.
	 * If no suitable Converter is found, and a {@link #setParent parent} is set, then queries the parent.
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
			return getDefaultConverter(sourceType, targetType);
		}
	}

	/**
	 * Return the default converter if no converter is found for the given sourceType/targetType pair.
	 * Returns a NO_OP Converter if the sourceType is assignalbe to the targetType.
	 * Returns <code>null</code> otherwise, indicating no suitable converter could be found.
	 * Subclasses may override.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the default generic converter that will perform the conversion
	 */
	protected GenericConverter getDefaultConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(targetType)) {
			return NO_OP_CONVERTER;
		} else {
			return null;
		}
	}

	// internal helpers

	private void assertNotNull(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(sourceType, "The sourceType to convert to is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
	}

	private Class<?>[] getRequiredTypeInfo(Object converter, Class<?> genericIfc) {
		return GenericTypeResolver.resolveTypeArguments(converter.getClass(), genericIfc);
	}

	private GenericConverter findConverterByClassPair(Class<?> sourceType, Class<?> targetType) {
		if (sourceType.isInterface()) {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				Map<Class<?>, GenericConverter> converters = getConvertersForSource(currentClass);
				GenericConverter converter = getConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				Class<?>[] interfaces = currentClass.getInterfaces();
				for (Class<?> ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			Map<Class<?>, GenericConverter> objectConverters = getConvertersForSource(Object.class);
			return getConverter(objectConverters, targetType);
		} else {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(sourceType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				Map<Class<?>, GenericConverter> converters = getConvertersForSource(currentClass);
				GenericConverter converter = getConverter(converters, targetType);
				if (converter != null) {
					return converter;
				}
				if (currentClass.isArray()) {
					Class<?> componentType = ClassUtils.resolvePrimitiveIfNecessary(currentClass.getComponentType());
					if (componentType.getSuperclass() != null) {
						classQueue.addFirst(Array.newInstance(componentType.getSuperclass(), 0).getClass());
					}
				} else {
					Class<?>[] interfaces = currentClass.getInterfaces();
					for (Class<?> ifc : interfaces) {
						classQueue.addFirst(ifc);
					}
					if (currentClass.getSuperclass() != null) {
						classQueue.addFirst(currentClass.getSuperclass());
					}
				}
			}
			return null;
		}
	}

	private Map<Class<?>, GenericConverter> getSourceMap(Class<?> sourceType) {
		Map<Class<?>, GenericConverter> sourceMap = sourceTypeConverters.get(sourceType);
		if (sourceMap == null) {
			sourceMap = new HashMap<Class<?>, GenericConverter>();
			this.sourceTypeConverters.put(sourceType, sourceMap);
		}
		return sourceMap;
	}

	private Map<Class<?>, GenericConverter> getConvertersForSource(Class<?> sourceType) {
		Map<Class<?>, GenericConverter> converters = this.sourceTypeConverters.get(sourceType);
		if (converters == null) {
			converters = Collections.emptyMap();
		}
		return converters;
	}

	private GenericConverter getConverter(Map<Class<?>, GenericConverter> converters, Class<?> targetType) {
		if (targetType.isInterface()) {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				GenericConverter converter = converters.get(currentClass);
				if (converter != null) {
					return converter;
				}
				Class<?>[] interfaces = currentClass.getInterfaces();
				for (Class<?> ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return converters.get(Object.class);
		} else {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(targetType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				GenericConverter converter = converters.get(currentClass);
				if (converter != null) {
					return converter;
				}
				if (currentClass.isArray()) {
					Class<?> componentType = ClassUtils.resolvePrimitiveIfNecessary(currentClass.getComponentType());
					if (componentType.getSuperclass() != null) {
						classQueue.addFirst(Array.newInstance(componentType.getSuperclass(), 0).getClass());
					}
				} else {
					Class<?>[] interfaces = currentClass.getInterfaces();
					for (Class<?> ifc : interfaces) {
						classQueue.addFirst(ifc);
					}
					if (currentClass.getSuperclass() != null) {
						classQueue.addFirst(currentClass.getSuperclass());
					}
				}
			}
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private final class ConverterAdapter implements GenericConverter {

		private final Converter converter;

		public ConverterAdapter(Converter<?, ?> converter) {
			this.converter = converter;
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converter.convert(source);
		}
	}

	@SuppressWarnings("unchecked")
	private final class ConverterFactoryAdapter implements GenericConverter {

		private final ConverterFactory converterFactory;

		public ConverterFactoryAdapter(ConverterFactory<?, ?> converterFactory) {
			this.converterFactory = converterFactory;
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}
	}

}
