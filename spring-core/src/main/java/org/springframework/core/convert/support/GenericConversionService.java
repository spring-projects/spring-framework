/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.converter.GenericConverter.ConvertiblePair;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base {@link ConversionService} implementation suitable for use in most environments.
 * Indirectly implements {@link ConverterRegistry} as registration API through the
 * {@link ConfigurableConversionService} interface.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 */
public class GenericConversionService implements ConfigurableConversionService {

	/**
	 * General NO-OP converter used when conversion is not required.
	 */
	private static final GenericConverter NO_OP_CONVERTER = new NoOpConverter("NO_OP");

	/**
	 * Used as a cache entry when no converter is available.  This converter is never
	 * returned.
	 */
	private static final GenericConverter NO_MATCH = new NoOpConverter("NO_MATCH");


	private final Converters converters = new Converters();

	private final Map<ConverterCacheKey, GenericConverter> converterCache =
			new ConcurrentHashMap<ConverterCacheKey, GenericConverter>();


	// implementing ConverterRegistry

	public void addConverter(Converter<?, ?> converter) {
		GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converter, Converter.class);
		Assert.notNull(typeInfo, "Unable to the determine sourceType <S> and targetType " +
				"<T> which your Converter<S, T> converts between; declare these generic types.");
		addConverter(new ConverterAdapter(typeInfo, converter));
	}

	public void addConverter(Class<?> sourceType, Class<?> targetType, Converter<?, ?> converter) {
		GenericConverter.ConvertiblePair typeInfo = new GenericConverter.ConvertiblePair(sourceType, targetType);
		addConverter(new ConverterAdapter(typeInfo, converter));
	}

	public void addConverter(GenericConverter converter) {
		this.converters.add(converter);
		invalidateCache();
	}

	public void addConverterFactory(ConverterFactory<?, ?> converterFactory) {
		GenericConverter.ConvertiblePair typeInfo = getRequiredTypeInfo(converterFactory, ConverterFactory.class);
		if (typeInfo == null) {
			throw new IllegalArgumentException("Unable to the determine sourceType <S> and " +
					"targetRangeType R which your ConverterFactory<S, R> converts between; " +
					"declare these generic types.");
		}
		addConverter(new ConverterFactoryAdapter(typeInfo, converterFactory));
	}

	public void removeConvertible(Class<?> sourceType, Class<?> targetType) {
		this.converters.remove(sourceType, targetType);
		invalidateCache();
	}

	// implementing ConversionService

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		Assert.notNull(targetType, "The targetType to convert to cannot be null");
		return canConvert(sourceType != null ?
				TypeDescriptor.valueOf(sourceType) : null,
				TypeDescriptor.valueOf(targetType));
	}

	public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType,"The targetType to convert to cannot be null");
		if (sourceType == null) {
			return true;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		return (converter != null);
	}

	/**
	 * Returns true if conversion between the sourceType and targetType can be bypassed.
	 * More precisely this method will return true if objects of sourceType can be
	 * converted to the targetType by returning the source object unchanged.
	 * @param sourceType context about the source type to convert from (may be null if source is null)
	 * @param targetType context about the target type to convert to (required)
	 * @return true if conversion can be bypassed
	 * @throws IllegalArgumentException if targetType is null
	 * @since 3.2
	 */
	public boolean canBypassConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType, "The targetType to convert to cannot be null");
		if (sourceType == null) {
			return true;
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		return (converter == NO_OP_CONVERTER);
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Class<T> targetType) {
		Assert.notNull(targetType,"The targetType to convert to cannot be null");
		return (T) convert(source, TypeDescriptor.forObject(source), TypeDescriptor.valueOf(targetType));
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		Assert.notNull(targetType,"The targetType to convert to cannot be null");
		if (sourceType == null) {
			Assert.isTrue(source == null, "The source must be [null] if sourceType == [null]");
			return handleResult(sourceType, targetType, convertNullSource(sourceType, targetType));
		}
		if (source != null && !sourceType.getObjectType().isInstance(source)) {
			throw new IllegalArgumentException("The source to convert from must be an instance of " +
					sourceType + "; instead it was a " + source.getClass().getName());
		}
		GenericConverter converter = getConverter(sourceType, targetType);
		if (converter != null) {
			Object result = ConversionUtils.invokeConverter(converter, source, sourceType, targetType);
			return handleResult(sourceType, targetType, result);
		}
		return handleConverterNotFound(source, sourceType, targetType);
	}

	/**
	 * Convenience operation for converting a source object to the specified targetType,
	 * where the targetType is a descriptor that provides additional conversion context.
	 * Simply delegates to {@link #convert(Object, TypeDescriptor, TypeDescriptor)} and
	 * encapsulates the construction of the sourceType descriptor using
	 * {@link TypeDescriptor#forObject(Object)}.
	 * @param source the source object
	 * @param targetType the target type
	 * @return the converted value
	 * @throws ConversionException if a conversion exception occurred
	 * @throws IllegalArgumentException if targetType is null
	 * @throws IllegalArgumentException if sourceType is null but source is not null
	 */
	public Object convert(Object source, TypeDescriptor targetType) {
		return convert(source, TypeDescriptor.forObject(source), targetType);
	}

	public String toString() {
		return this.converters.toString();
	}


	// subclassing hooks

	/**
	 * Template method to convert a null source.
	 * <p>Default implementation returns <code>null</code>.
	 * Subclasses may override to return custom null objects for specific target types.
	 * @param sourceType the sourceType to convert from
	 * @param targetType the targetType to convert to
	 * @return the converted null object
	 */
	protected Object convertNullSource(TypeDescriptor sourceType, TypeDescriptor targetType) {
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
	 * @return the generic converter that will perform the conversion, or {@code null} if
	 * no suitable converter was found
	 * @see #getDefaultConverter(TypeDescriptor, TypeDescriptor)
	 */
	protected GenericConverter getConverter(TypeDescriptor sourceType, TypeDescriptor targetType) {
		ConverterCacheKey key = new ConverterCacheKey(sourceType, targetType);
		GenericConverter converter = this.converterCache.get(key);
		if (converter != null) {
			return (converter != NO_MATCH ? converter : null);
		}

		converter = this.converters.find(sourceType, targetType);
		if (converter == null) {
			converter = getDefaultConverter(sourceType, targetType);
		}

		if (converter != null) {
			this.converterCache.put(key, converter);
			return converter;
		}

		this.converterCache.put(key, NO_MATCH);
		return null;
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
		return (sourceType.isAssignableTo(targetType) ? NO_OP_CONVERTER : null);
	}

	// internal helpers

	private GenericConverter.ConvertiblePair getRequiredTypeInfo(Object converter, Class<?> genericIfc) {
		Class<?>[] args = GenericTypeResolver.resolveTypeArguments(converter.getClass(), genericIfc);
		return (args != null ? new GenericConverter.ConvertiblePair(args[0], args[1]) : null);
	}

	private void invalidateCache() {
		this.converterCache.clear();
	}

	private Object handleConverterNotFound(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			assertNotPrimitiveTargetType(sourceType, targetType);
			return source;
		}
		if (sourceType.isAssignableTo(targetType) && targetType.getObjectType().isInstance(source)) {
			return source;
		}
		throw new ConverterNotFoundException(sourceType, targetType);
	}

	private Object handleResult(TypeDescriptor sourceType, TypeDescriptor targetType, Object result) {
		if (result == null) {
			assertNotPrimitiveTargetType(sourceType, targetType);
		}
		return result;
	}

	private void assertNotPrimitiveTargetType(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.isPrimitive()) {
			throw new ConversionFailedException(sourceType, targetType, null,
					new IllegalArgumentException("A null value cannot be assigned to a primitive type"));
		}
	}


	/**
	 * Adapts a {@link Converter} to a {@link GenericConverter}.
	 */
	@SuppressWarnings("unchecked")
	private final class ConverterAdapter implements ConditionalGenericConverter {

		private final ConvertiblePair typeInfo;

		private final Converter<Object, Object> converter;


		public ConverterAdapter(ConvertiblePair typeInfo, Converter<?, ?> converter) {
			this.converter = (Converter<Object, Object>) converter;
			this.typeInfo = typeInfo;
		}


		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			if(!this.typeInfo.getTargetType().equals(targetType.getObjectType())) {
				return false;
			}
			if (this.converter instanceof ConditionalConverter) {
				return ((ConditionalConverter) this.converter).matches(sourceType, targetType);
			}
			return true;
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converter.convert(source);
		}

		public String toString() {
			return this.typeInfo.getSourceType().getName() + " -> " +
					this.typeInfo.getTargetType().getName() + " : " +
					this.converter.toString();
		}
	}


	/**
	 * Adapts a {@link ConverterFactory} to a {@link GenericConverter}.
	 */
	@SuppressWarnings("unchecked")
	private final class ConverterFactoryAdapter implements ConditionalGenericConverter {

		private final ConvertiblePair typeInfo;

		private final ConverterFactory<Object, Object> converterFactory;


		public ConverterFactoryAdapter(ConvertiblePair typeInfo, ConverterFactory<?, ?> converterFactory) {
			this.converterFactory = (ConverterFactory<Object, Object>) converterFactory;
			this.typeInfo = typeInfo;
		}


		public Set<ConvertiblePair> getConvertibleTypes() {
			return Collections.singleton(this.typeInfo);
		}

		public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
			boolean matches = true;
			if (this.converterFactory instanceof ConditionalConverter) {
				matches = ((ConditionalConverter) this.converterFactory).matches(sourceType, targetType);
			}
			if(matches) {
				Converter<?, ?> converter = this.converterFactory.getConverter(targetType.getType());
				if(converter instanceof ConditionalConverter) {
					matches = ((ConditionalConverter) converter).matches(sourceType, targetType);
				}
			}
			return matches;
		}

		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return convertNullSource(sourceType, targetType);
			}
			return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
		}

		public String toString() {
			return this.typeInfo.getSourceType().getName() + " -> " +
					this.typeInfo.getTargetType().getName() + " : " +
					this.converterFactory.toString();
		}
	}


	/**
	 * Key for use with the converter cache.
	 */
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
			return ObjectUtils.nullSafeEquals(this.sourceType, otherKey.sourceType)
				&& ObjectUtils.nullSafeEquals(this.targetType, otherKey.targetType);
		}

		public int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.sourceType) * 29
				+ ObjectUtils.nullSafeHashCode(this.targetType);
		}

		public String toString() {
			return "ConverterCacheKey [sourceType = " + this.sourceType
				+ ", targetType = " + this.targetType + "]";
		}
	}

	/**
	 * Manages all converters registered with the service.
	 */
	private static class Converters {

		private static final Set<Class<?>> IGNORED_CLASSES;
		static {
			Set<Class<?>> ignored = new HashSet<Class<?>>();
			ignored.add(Object.class);
			ignored.add(Object[].class);
			IGNORED_CLASSES = Collections.unmodifiableSet(ignored);
		}

		private final Set<GenericConverter> globalConverters =
				new LinkedHashSet<GenericConverter>();

		private final Map<ConvertiblePair, ConvertersForPair> converters =
			new LinkedHashMap<ConvertiblePair, ConvertersForPair>(36);

		public void add(GenericConverter converter) {
			Set<ConvertiblePair> convertibleTypes = converter.getConvertibleTypes();
			if (convertibleTypes == null) {
				Assert.state(converter instanceof ConditionalConverter,
						"Only conditional converters may return null convertible types");
				globalConverters.add(converter);
			} else {
				for (ConvertiblePair convertiblePair : convertibleTypes) {
					ConvertersForPair convertersForPair = getMatchableConverters(convertiblePair);
					convertersForPair.add(converter);
				}
			}
		}

		private ConvertersForPair getMatchableConverters(ConvertiblePair convertiblePair) {
			ConvertersForPair convertersForPair = this.converters.get(convertiblePair);
			if (convertersForPair == null) {
				convertersForPair = new ConvertersForPair();
				this.converters.put(convertiblePair, convertersForPair);
			}
			return convertersForPair;
		}

		public void remove(Class<?> sourceType, Class<?> targetType) {
			converters.remove(new ConvertiblePair(sourceType, targetType));
		}

		/**
		 * Find a {@link GenericConverter} given a source and target type.  This method will
		 * attempt to match all possible converters by working though the class and interface
		 * hierarchy of the types.
		 * @param sourceType the source type
		 * @param targetType the target type
		 * @return a {@link GenericConverter} or <tt>null</tt>
		 * @see #getTypeHierarchy(Class)
		 */
		public GenericConverter find(TypeDescriptor sourceType, TypeDescriptor targetType) {
			// Search the full type hierarchy
			List<TypeDescriptor> sourceCandidates = getTypeHierarchy(sourceType);
			List<TypeDescriptor> targetCandidates = getTypeHierarchy(targetType);
			for (TypeDescriptor sourceCandidate : sourceCandidates) {
				for (TypeDescriptor targetCandidate : targetCandidates) {
					GenericConverter converter = getRegisteredConverter(
							sourceType, targetType, sourceCandidate, targetCandidate);
					if(converter != null) {
						return converter;
					}
				}
			}
			return null;
		}

		private GenericConverter getRegisteredConverter(TypeDescriptor sourceType, TypeDescriptor targetType,
				TypeDescriptor sourceCandidate, TypeDescriptor targetCandidate) {

			// Check specifically registered converters
			ConvertersForPair convertersForPair = converters.get(new ConvertiblePair(
				sourceCandidate.getType(), targetCandidate.getType()));
			GenericConverter converter = convertersForPair == null ? null
				: convertersForPair.getConverter(sourceType, targetType);
			if (converter != null) {
				return converter;
			}

			// Check ConditionalGenericConverter that match all types
			for (GenericConverter globalConverter : this.globalConverters) {
				if (((ConditionalConverter)globalConverter).matches(
						sourceCandidate, targetCandidate)) {
					return globalConverter;
				}
			}

			return null;
		}

		/**
		 * Returns an ordered class hierarchy for the given type.
		 * @param type the type
		 * @return an ordered list of all classes that the given type extends or
		 *         implements.
		 */
		private List<TypeDescriptor> getTypeHierarchy(TypeDescriptor type) {
			if(type.isPrimitive()) {
				type = TypeDescriptor.valueOf(type.getObjectType());
			}
			Set<TypeDescriptor> typeHierarchy = new LinkedHashSet<TypeDescriptor>();
			collectTypeHierarchy(typeHierarchy, type);
			if(type.isArray()) {
				typeHierarchy.add(TypeDescriptor.valueOf(Object[].class));
			}
			typeHierarchy.add(TypeDescriptor.valueOf(Object.class));
			return new ArrayList<TypeDescriptor>(typeHierarchy);
		}

		private void collectTypeHierarchy(Set<TypeDescriptor> typeHierarchy,
				TypeDescriptor type) {
			if(type != null && !IGNORED_CLASSES.contains(type.getType())) {
				if(typeHierarchy.add(type)) {
					Class<?> superclass = type.getType().getSuperclass();
					if (type.isArray()) {
						superclass = ClassUtils.resolvePrimitiveIfNecessary(superclass);
					}
					collectTypeHierarchy(typeHierarchy, createRelated(type, superclass));

					for (Class<?> implementsInterface : type.getType().getInterfaces()) {
						collectTypeHierarchy(typeHierarchy, createRelated(type, implementsInterface));
					}
				}
			}
		}

		private TypeDescriptor createRelated(TypeDescriptor type, Class<?> relatedType) {
			if (relatedType == null && type.isArray()) {
				relatedType = Array.newInstance(relatedType, 0).getClass();
			}
			if(!type.getType().equals(relatedType)) {
				return type.upcast(relatedType);
			}
			return null;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ConversionService converters = ").append("\n");
			for (String converterString : getConverterStrings()) {
				builder.append("\t");
				builder.append(converterString);
				builder.append("\n");
			}
			return builder.toString();
		}

		private List<String> getConverterStrings() {
			List<String> converterStrings = new ArrayList<String>();
			for (ConvertersForPair convertersForPair : converters.values()) {
				converterStrings.add(convertersForPair.toString());
			}
			Collections.sort(converterStrings);
			return converterStrings;
		}
	}


	/**
	 * Manages converters registered with a specific {@link ConvertiblePair}.
	 */
	private static class ConvertersForPair {

		private final LinkedList<GenericConverter> converters = new LinkedList<GenericConverter>();

		public void add(GenericConverter converter) {
			this.converters.addFirst(converter);
		}

		public GenericConverter getConverter(TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			for (GenericConverter converter : this.converters) {
				if (!(converter instanceof ConditionalGenericConverter)
						|| ((ConditionalGenericConverter) converter).matches(sourceType,
								targetType)) {
					return converter;
				}
			}
			return null;
		}

		public String toString() {
			return StringUtils.collectionToCommaDelimitedString(this.converters);
		}
	}


	/**
	 * Internal converter that performs no operation.
	 */
	private static class NoOpConverter implements GenericConverter {

		private String name;


		public NoOpConverter(String name) {
			this.name = name;
		}


		public Set<ConvertiblePair> getConvertibleTypes() {
			return null;
		}

		public Object convert(Object source, TypeDescriptor sourceType,
				TypeDescriptor targetType) {
			return source;
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
