/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.concurrent.ConcurrentHashMap;

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
import org.springframework.core.style.StylerUtils;
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

	private static final GenericConverter NO_OP_CONVERTER = new GenericConverter() {
		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			return source;
		}
		public String toString() {
			return "NO_OP";
		}
	};

	private static final GenericConverter NO_MATCH = new GenericConverter() {
		public Set<ConvertiblePair> getConvertibleTypes() {
			throw new UnsupportedOperationException();
		}
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			throw new UnsupportedOperationException();
		}
		public String toString() {
			return "NO_MATCH";
		}
	};


	private static final Log logger = LogFactory.getLog(GenericConversionService.class);

	private final Map<Class<?>, Map<Class<?>, MatchableConverters>> converters =
			new HashMap<Class<?>, Map<Class<?>, MatchableConverters>>(36);

	private final Map<ConverterCacheKey, GenericConverter> converterCache =
			new ConcurrentHashMap<ConverterCacheKey, GenericConverter>();


	// implementing ConverterRegistry

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
	
	public void addConverter(GenericConverter converter) {
		Set<GenericConverter.ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
		for (GenericConverter.ConvertiblePair convertibleType : convertibleTypes) {
			getMatchableConverters(convertibleType.getSourceType(), convertibleType.getTargetType()).add(converter);
		}
		invalidateCache();
	}

	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		getSourceConverterMap(sourceType).remove(targetType);
		invalidateCache();
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
		if (logger.isTraceEnabled()) {
			logger.trace("Checking if I can convert " + sourceType + " to " + targetType);
		}		
		if (sourceType == TypeDescriptor.NULL || targetType == TypeDescriptor.NULL) {
			logger.trace("Yes, I can convert");
			return true;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter != null) {
			logger.trace("Yes, I can convert");
			return true;
		}
		else {
			logger.trace("No, I cannot convert");
			return false;
		}
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		assertNotNull(sourceType, targetType);
		if (logger.isDebugEnabled()) {
			logger.debug("Converting value " + StylerUtils.style(source) + " of " + sourceType + " to " + targetType);
		}
		if (sourceType == TypeDescriptor.NULL) {
			Assert.isTrue(source == null, "The value must be null if sourceType == TypeDescriptor.NULL");
			Object result = convertNullSource(sourceType, targetType);
			if (logger.isDebugEnabled()) {
				logger.debug("Converted to " + StylerUtils.style(result));
			}			
			return result;
		}
		if (targetType == TypeDescriptor.NULL) {
			logger.debug("Converted to null");
			return null;
		}
		Assert.isTrue(source == null || sourceType.getObjectType().isInstance(source));
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter == null) {
			if (source == null || targetType.getObjectType().isInstance(source)) {
				logger.debug("No converter found - returning assignable source object as-is");
				return source;
			}
			else {
				throw new ConverterNotFoundException(sourceType, targetType);
			}
		}
		Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
		if (logger.isDebugEnabled()) {
			logger.debug("Converted to " + StylerUtils.style(result));
		}
		return result;
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
	 * Template method to convert a null source.
	 * <p>Default implementation returns <code>null</code>.
	 * Throws a {@link ConversionFailedException} if the targetType is a primitive type,
	 * as <code>null</code> cannot be assigned to a primitive type.
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
	 * First queries this ConversionService's converter cache.
	 * On a cache miss, then performs an exhaustive search for a matching converter.
	 * If no converter matches, returns the default converter.
	 * Subclasses may override.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the generic converter that will perform the conversion, or <code>null</code> if no suitable converter was found
	 * @see #getDefaultConverter(TypeDescriptor, TypeDescriptor)
	 */
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
		GenericConverter converter = this.converterCache.get(key);
		if (converter != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Matched cached converter " + converter);
			}
			return (converter != NO_MATCH ? converter : null);
		}
		else {
			converter = findConverterForClassPair(sourceType, targetType);
			if (converter != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Caching under " + key);
				}
				this.converterCache.put(key, converter);
				return converter;
			}
			converter = getDefaultConverter(sourceType, targetType);
			if (converter != null) {
				if (logger.isTraceEnabled()) {
					logger.trace("Caching under " + key);
				}
				this.converterCache.put(key, converter);
				return converter;				
			}
			if (logger.isTraceEnabled()) {
				logger.trace("Caching NO_MATCH under " + key);
			}
			this.converterCache.put(key, NO_MATCH);
			return null;
		}
	}

	/**
	 * Return the default converter if no converter is found for the given sourceType/targetType pair.
	 * Returns a NO_OP Converter if the sourceType is assignable to the targetType.
	 * Returns <code>null</code> otherwise, indicating no suitable converter could be found.
	 * Subclasses may override.
	 * @param sourceType the source type to convert from
	 * @param targetType the target type to convert to
	 * @return the default generic converter that will perform the conversion
	 */
	protected GenericConverter getDefaultConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(targetType)) {
			logger.trace("Matched default NO_OP_CONVERTER");
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

	private MatchableConverters getMatchableConverters(Class<?> sourceType, Class<?> targetType) {
		Map<Class<?>, MatchableConverters> sourceMap = getSourceConverterMap(sourceType);
		MatchableConverters matchable = sourceMap.get(targetType);
		if (matchable == null) {
			matchable = new MatchableConverters();
			sourceMap.put(targetType, matchable);
		}
		return matchable;
	}
	
	private void invalidateCache() {
		this.converterCache.clear();
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
		Class<?> sourceObjectType = sourceType.getObjectType();
		if (sourceObjectType.isInterface()) {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(sourceObjectType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				if (logger.isTraceEnabled()) {
					logger.trace("Searching for converters indexed by sourceType [" + currentClass.getName() + "]");
				}
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
				if (logger.isTraceEnabled()) {
					logger.trace("Searching for converters indexed by sourceType [" + currentClass.getName() + "]");
				}
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
					else if (componentType.isInterface()) {
						classQueue.addFirst(Object[].class);
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
				if (logger.isTraceEnabled()) {
					logger.trace("and indexed by targetType [" + currentClass.getName() + "]");
				}
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
			if (logger.isTraceEnabled()) {
				logger.trace("and indexed by [java.lang.Object]");
			}							
			return matchConverter(converters.get(Object.class), sourceType, targetType);
		}
		else {
			LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
			classQueue.addFirst(targetObjectType);
			while (!classQueue.isEmpty()) {
				Class<?> currentClass = classQueue.removeLast();
				if (logger.isTraceEnabled()) {
					logger.trace("and indexed by targetType [" + currentClass.getName() + "]");
				}				
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
					else if (componentType.isInterface()) {
						classQueue.addFirst(Object[].class);
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

		if (matchable == null) {
			return null;
		}
		if (logger.isTraceEnabled()) {
			logger.trace("Found matchable converters " + matchable);
		}
		return matchable.matchConverter(sourceFieldType, targetFieldType);
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
					if (logger.isTraceEnabled()) {
						logger.trace("Matching " + conditional);
					}
					if (conditional.matches(sourceType, targetType)) {
						if (logger.isTraceEnabled()) {
							logger.trace("Matched converter " + conditional);
						}
						return conditional;
					}
					else {
						if (logger.isTraceEnabled()) {
							logger.trace("Did not match converter " + conditional);
						}
					}
				}
			}
			if (this.defaultConverter != null && logger.isTraceEnabled()) {
				logger.trace("Matched converter " + this.defaultConverter);
			}			
			return this.defaultConverter;
		}

		public String toString() {
			if (this.conditionalConverters != null) {
				StringBuilder builder = new StringBuilder();
				for (Iterator<ConditionalGenericConverter> it = this.conditionalConverters.iterator(); it.hasNext();) {
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


	private static final class ConverterCacheKey {

		private final TypeDescriptor sourceType;
		
		private final TypeDescriptor targetType;
		
		public ConverterCacheKey(TypeDescriptor sourceType, TypeDescriptor targetType) {
			this.sourceType = sourceType;
			this.targetType = targetType;
		}
		
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof ConverterCacheKey)) {
				return false;
			}
			ConverterCacheKey otherKey = (ConverterCacheKey) other;
			return this.sourceType.equals(otherKey.sourceType) && this.targetType.equals(otherKey.targetType);
		}
		
		public int hashCode() {
			return this.sourceType.hashCode() * 29 + this.targetType.hashCode();
		}
		
		public String toString() {
			return "ConverterCacheKey [sourceType = " + this.sourceType + ", targetType = " + this.targetType + "]";
		}
	}

}
