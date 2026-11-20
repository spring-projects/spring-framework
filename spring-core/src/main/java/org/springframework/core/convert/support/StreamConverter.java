/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts a {@link Stream} to and from a collection or array, converting the
 * element type if necessary.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
class StreamConverter implements ConditionalGenericConverter {

	private static final TypeDescriptor STREAM_TYPE = TypeDescriptor.valueOf(Stream.class);

	private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = createConvertibleTypes();

	private final ConversionService conversionService;


	public StreamConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		return CONVERTIBLE_TYPES;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return matchesFromStream(sourceType.getElementTypeDescriptor(), targetType);
		}
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return matchesToStream(targetType.getElementTypeDescriptor(), sourceType);
		}
		return false;
	}

	/**
	 * Validate that a {@link Collection} of the elements held within the stream can be
	 * converted to the specified {@code targetType}.
	 * @param elementType the type of the stream elements
	 * @param targetType the type to convert to
	 */
	public boolean matchesFromStream(@Nullable TypeDescriptor elementType, TypeDescriptor targetType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(collectionOfElement, targetType);
	}

	/**
	 * Validate that the specified {@code sourceType} can be converted to a {@link Collection} of
	 * the type of the stream elements.
	 * @param elementType the type of the stream elements
	 * @param sourceType the type to convert from
	 */
	public boolean matchesToStream(@Nullable TypeDescriptor elementType, TypeDescriptor sourceType) {
		TypeDescriptor collectionOfElement = TypeDescriptor.collection(Collection.class, elementType);
		return this.conversionService.canConvert(sourceType, collectionOfElement);
	}

	@Override
	public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (sourceType.isAssignableTo(STREAM_TYPE)) {
			return convertFromStream((Stream<?>) source, sourceType, targetType);
		}
		if (targetType.isAssignableTo(STREAM_TYPE)) {
			return convertToStream(source, sourceType, targetType);
		}
		// Should not happen
		throw new IllegalStateException("Unexpected source/target types");
	}

	private @Nullable Object convertFromStream(@Nullable Stream<?> source, TypeDescriptor streamType, TypeDescriptor targetType) {
		List<Object> content = (source != null ? source.collect(Collectors.<Object>toList()) : Collections.emptyList());
		TypeDescriptor listType = TypeDescriptor.collection(List.class, streamType.getElementTypeDescriptor());
		return this.conversionService.convert(content, listType, targetType);
	}

	private Object convertToStream(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor streamType) {
		TypeDescriptor targetCollection = TypeDescriptor.collection(List.class, streamType.getElementTypeDescriptor());
		List<?> target = (List<?>) this.conversionService.convert(source, sourceType, targetCollection);
		if (target == null) {
			target = Collections.emptyList();
		}
		return target.stream();
	}


	private static Set<ConvertiblePair> createConvertibleTypes() {
		Set<ConvertiblePair> convertiblePairs = new HashSet<>();
		convertiblePairs.add(new ConvertiblePair(Stream.class, Collection.class));
		convertiblePairs.add(new ConvertiblePair(Stream.class, Object[].class));
		convertiblePairs.add(new ConvertiblePair(Collection.class, Stream.class));
		convertiblePairs.add(new ConvertiblePair(Object[].class, Stream.class));
		return convertiblePairs;
	}

}
