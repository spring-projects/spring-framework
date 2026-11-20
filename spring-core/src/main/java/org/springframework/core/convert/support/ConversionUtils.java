/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ClassUtils;

/**
 * Internal utilities for the conversion package.
 *
 * @author Keith Donald
 * @author Stephane Nicoll
 * @since 3.0
 */
abstract class ConversionUtils {

	public static @Nullable Object invokeConverter(GenericConverter converter, @Nullable Object source,
			TypeDescriptor sourceType, TypeDescriptor targetType) {

		try {
			return converter.convert(source, sourceType, targetType);
		}
		catch (ConversionFailedException ex) {
			throw ex;
		}
		catch (Throwable ex) {
			throw new ConversionFailedException(sourceType, targetType, source, ex);
		}
	}

	public static boolean canConvertElements(@Nullable TypeDescriptor sourceElementType,
			@Nullable TypeDescriptor targetElementType, ConversionService conversionService) {

		if (targetElementType == null) {
			// yes
			return true;
		}
		if (sourceElementType == null) {
			// maybe
			return true;
		}
		if (conversionService.canConvert(sourceElementType, targetElementType)) {
			// yes
			return true;
		}
		if (ClassUtils.isAssignable(sourceElementType.getType(), targetElementType.getType())) {
			// maybe
			return true;
		}
		// no
		return false;
	}

	/**
	 * Resolve the enum type for the supplied target type.
	 * @param targetType the target type for which to resolve the enum type
	 * @return the resolved enum type, or {@code null} if the supplied target type
	 * does not refer to an enum
	 */
	public static @Nullable Class<?> resolveEnumType(Class<?> targetType) {
		Class<?> enumType = targetType;
		while (enumType != null && !enumType.isEnum()) {
			enumType = enumType.getSuperclass();
		}
		return enumType;
	}

}
