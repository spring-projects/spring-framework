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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Base {@link ConversionService} implementation suitable for use in most environments.
 * Implements {@link ConverterRegistry} as registration API.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
public class GenericConversionService implements ConversionService, ConverterRegistry {

	private static final Log logger = LogFactory.getLog(GenericConversionService.class);

	private static final GenericConverter NO_OP_CONVERTER = new GenericConverter() {
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return source;
		}
	};


	private final Map<Class<?>, Map<Class<?>, MatchableConverters>> converters =
			new HashMap<Class<?>, Map<Class<?>, MatchableConverters>>(36);


	// implementing ConverterRegistry

	public void addConverter(GenericConverter converter) {
		Set<GenericConverter.ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
		for (GenericConverter.ConvertiblePair convertibleType : convertibleTypes) {
			getMatchableConvertersList(convertibleType.getSourceType(), convertibleType.getTargetType()).add(converter);
		}
	}

	public void addConverter(Converter<?, ?> converter) {
		GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converter, Converter.class);
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to the determine sourceType <S> and targetType <T> which " +
							"your Converter<S, T> converts between; declare these generic types.");
		}
		addConverter(new ConverterAdapter(typeInfo, converter));
	}

	public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
		GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converterFactory, ConverterFactory.class);
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to the determine sourceType <S> and targetRangeType R which " +
					"your ConverterFactory<S, R> converts between; declare these generic types.");
		}
		addConverter(new ConverterFactoryAdapter(typeInfo, converterFactory));
	}

	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		getSourceConverterMap(sourceType).remove(targetType);
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
		return ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
	}

	public String toString() {
		List<String> converterStrings = new ArrayList<String>();
		for (Map<Class<?>, MatchableConverters> targetConverters : this.converters.values()) {
			for (MatchableConverters matchable : targetConverters.values()) {
				converterStrings.add(matchable.toString());
			}
		}
		Collections.sort(converterStrings);
		StringBuilder builder = new StringBuilder();
		builder.append("ConversionService converters = ").append("\n");
		for (String converterString : converterStrings) {
			builder.append("\t");
			builder.append(converterString);
			builder.append("\n");			
		}
		return builder.toString();
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
			throw new ConversionFailedException(sourceType, targetType, null,
					new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
		}
		return null;
	}

	/**
	 * Hook method to lookup the converter for a given sourceType/targetType pair.
	 * First queries this ConversionService's converter map.
	 * <p>Returns <code>null</code> if this ConversionService simply cannot convert
	 * between sourceType and targetType. Subclasses may override.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the generic converter that will perform the conversion, or <code>null</code> if no suitable converter was found
	 */
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		GenericConverter converter = findConverterForClassPair(sourceType, targetType);
		if (converter != null) {
			return converter;
		}
		else {
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
		}
		else {
			return null;
		}
	}


	// internal helpers

	private GenericConverter.ConvertiblePair getRequiredTypeInfo(Object converter, Class<?> genericIfc) {
		Class<?>[] args = GenericTypeResolver.resolveTypeArguments(converter.getClass(), genericIfc);
		return (args != null ? new GenericConverter.ConvertiblePair(args[0], args[1]) : null);
	}

	private MatchableConverters getMatchableConvertersList(Class<?> sourceType, Class<?> targetType) {
		Map<Class<?>, MatchableConverters> sourceMap = getSourceConverterMap(sourceType);
		MatchableConverters matchable = sourceMap.get(targetType);
		if (matchable == null) {
			matchable = new MatchableConverters();
			sourceMap.put(targetType, matchable);
		}
		return matchable;
	}

	private Map<Class<?>, MatchableConverters> getSourceConverterMap(Class<?> sourceType) {
		Map<Class<?>, MatchableConverters> sourceMap = converters.get(sourceType);
		if (sourceMap == null) {
			sourceMap = new HashMap<Class<?>, MatchableConverters>();
			this.converters.put(sourceType, sourceMap);
		}
		return sourceMap;
	}

	private void assertNotNull(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(sourceType, "The sourceType to convert to is required");
		Assert.notNull(targetType, "The targetType to convert to is required");
	}

	private GenericConverter findConverterForClassPair(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for Converter to convert from " + sourceType + " to " + targetType);
		}
		Class<?> sourceObjectType = sourceType.getObjectType();
		if (sourceObjectType.isInterface()) {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(sourceObjectType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				Map<Class<?>, MatchableConverters> converters = getTargetConvertersForSource(currentClass);
				GenericConverter converter = getMatchingConverterForTarget(sourceType, targetType, converters);
				if (converter != null) {
					return converter;
				}
				Class<?>[] interfaces = currentClass.getInterfaces();
				for (Class<?> ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			Map<Class<?>, MatchableConverters> objectConverters = getTargetConvertersForSource(Object.class);
			return getMatchingConverterForTarget(sourceType, targetType, objectConverters);
		}
		else {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(sourceObjectType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				Map<Class<?>, MatchableConverters> converters = getTargetConvertersForSource(currentClass);
				GenericConverter converter = getMatchingConverterForTarget(sourceType, targetType, converters);
				if (converter != null) {
					return converter;
				}
				if (currentClass.isArray()) {
					Class<?> componentType = ClassUtils.resolvePrimitiveIfNecessary(currentClass.getComponentType());
					if (componentType.getSuperclass() != null) {
						classQueue.addFirst(Array.newInstance(componentType.getSuperclass(), 0).getClass());
					}
				}
				else {
					Class<?>[] interfaces = currentClass.getInterfaces();
					for (Class<?> ifc : interfaces) {
						addInterfaceHierarchy(ifc, classQueue);
					}
					if (currentClass.getSuperclass() != null) {
						classQueue.addFirst(currentClass.getSuperclass());
					}
				}
			}
			return null;
		}
	}

	private Map<Class<?>, MatchableConverters> getTargetConvertersForSource(Class<?> sourceType) {
		Map<Class<?>, MatchableConverters> converters = this.converters.get(sourceType);
		if (converters == null) {
			converters = Collections.emptyMap();
		}
		return converters;
	}

	private GenericConverter getMatchingConverterForTarget(TypeDescriptor sourceType, TypeDescriptor targetType,
			Map<Class<?>, MatchableConverters> converters) {

		Class<?> targetObjectType = targetType.getObjectType();
		if (targetObjectType.isInterface()) {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(targetObjectType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				MatchableConverters matchable = converters.get(currentClass);
				GenericConverter converter = matchConverter(matchable, sourceType, targetType);
				if (converter != null) {
					return converter;
				}
				Class<?>[] interfaces = currentClass.getInterfaces();
				for (Class<?> ifc : interfaces) {
					classQueue.addFirst(ifc);
				}
			}
			return matchConverter(converters.get(Object.class), sourceType, targetType);
		}
		else {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(targetObjectType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				MatchableConverters matchable = converters.get(currentClass);
				GenericConverter converter = matchConverter(matchable, sourceType, targetType);
				if (converter != null) {
					return converter;
				}
				if (currentClass.isArray()) {
					Class<?> componentType = ClassUtils.resolvePrimitiveIfNecessary(currentClass.getComponentType());
					if (componentType.getSuperclass() != null) {
						classQueue.addFirst(Array.newInstance(componentType.getSuperclass(), 0).getClass());
					}
				}
				else {
					Class<?>[] interfaces = currentClass.getInterfaces();
					for (Class<?> ifc : interfaces) {
						addInterfaceHierarchy(ifc, classQueue);
					}
					if (currentClass.getSuperclass() != null) {
						classQueue.addFirst(currentClass.getSuperclass());
					}
				}
			}
			return null;
		}
	}

	private void addInterfaceHierarchy(Class<?> ifc, LinkedList<Class<?>> classQueue) {
		classQueue.addFirst(ifc);
		for (Class<?> inheritedIfc : ifc.getInterfaces()) {
			addInterfaceHierarchy(inheritedIfc, classQueue);
		}
	}

	private GenericConverter matchConverter(
			MatchableConverters matchable, TypeDescriptor sourceFieldType, TypeDescriptor targetFieldType) {

		return (matchable != null ? matchable.matchConverter(sourceFieldType, targetFieldType) : null);
	}

	@SuppressWarnings("unchecked")
	private final class ConverterAdapter implements GenericConverter {

		private final ConvertiblePair typeInfo;

		private final Converter converter;

		public ConverterAdapter(ConvertiblePair typeInfo, Converter<?, ?> converter) {
			this.converter = converter;
			this.typeInfo = typeInfo;
		}

		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converter.convert(source);
		}

		public String toString() {
			return this.typeInfo.getSourceType().getName() + " -> " + this.typeInfo.getTargetType().getName() +
					" : " + this.converter.toString();
		}
	}


	@SuppressWarnings("unchecked")
	private final class ConverterFactoryAdapter implements GenericConverter {

		private final ConvertiblePair typeInfo;

		private final ConverterFactory converterFactory;

		public ConverterFactoryAdapter(ConvertiblePair typeInfo, ConverterFactory<?, ?> converterFactory) {
			this.converterFactory = converterFactory;
			this.typeInfo = typeInfo;
		}

		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}

		public String toString() {
			return this.typeInfo.getSourceType().getName() + " -> " + this.typeInfo.getTargetType().getName() +
					" : " + this.converterFactory.toString();
		}
	}


	private static class MatchableConverters {

		private LinkedList<ConditionalGenericConverter> conditionalConverters;

		private GenericConverter defaultConverter;

		public void add(GenericConverter converter) {
			if (converter instanceof ConditionalGenericConverter) {
				if (this.conditionalConverters == null) {
					this.conditionalConverters = new LinkedList<ConditionalGenericConverter>();
				}
				this.conditionalConverters.addFirst((ConditionalGenericConverter) converter);
			}
			else {
				this.defaultConverter = converter;
			}
		}

		public GenericConverter matchConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (this.conditionalConverters != null) {
				for (ConditionalGenericConverter conditional : this.conditionalConverters) {
					if (conditional.matches(sourceType, targetType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Converter lookup [MATCHED] " + conditional);
						}
						return conditional;
					}
					else {
						if (logger.isDebugEnabled()) {
							logger.debug("Converter lookup [DID NOT MATCH] " + conditional);
						}
					}
				}
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Converter lookup [MATCHED] " + this.defaultConverter);
			}			
			return this.defaultConverter;
		}

		public String toString() {
			if (this.conditionalConverters != null) {
				StringBuilder builder = new StringBuilder();
				for (Iterator<ConditionalGenericConverter> it = this.conditionalConverters.iterator(); it.hasNext(); ) {
					builder.append(it.next());
					if (it.hasNext()) {
						builder.append(", ");
					}
				}
				if (this.defaultConverter != null) {
					builder.append(", ").append(this.defaultConverter);
				}
				return builder.toString();
			}
			else {
				return this.defaultConverter.toString();
			}
		}
	}

}
