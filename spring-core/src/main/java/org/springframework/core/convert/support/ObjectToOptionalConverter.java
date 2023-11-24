/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;
import org.springframework.lang.Nullable;

/**
 * Convert an Object to {@code java.util.Optional<T>} if necessary using the
 * {@code ConversionService} to convert the source Object to the generic type
 * of Optional when known.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 4.1
 */
final class ObjectToOptionalConverter implements ConditionalGenericConverter {

	private final ConversionService conversionService;


	public ObjectToOptionalConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}


	@Override
	public Set<ConvertiblePair> getConvertibleTypes() {
		Set<ConvertiblePair> convertibleTypes = new LinkedHashSet<>(4);
		convertibleTypes.add(new ConvertiblePair(Collection.class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object[].class, Optional.class));
		convertibleTypes.add(new ConvertiblePair(Object.class, Optional.class));
		return convertibleTypes;
	}

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.getResolvableType().hasGenerics()) {
			return this.conversionService.canConvert(sourceType, new GenericTypeDescriptor(targetType));
		}
		else {
			return true;
		}
	}

	@Override
	public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (source == null) {
			return Optional.empty();
		}
		else if (source instanceof Optional) {
			return source;
		}
		else if (targetType.getResolvableType().hasGenerics()) {
			Object target = this.conversionService.convert(source, sourceType, new GenericTypeDescriptor(targetType));
			if (target == null || (target.getClass().isArray() && Array.getLength(target) == 0) ||
						(target instanceof Collection<?> collection && collection.isEmpty())) {
				return Optional.empty();
			}
			return Optional.of(target);
		}
		else {
			return Optional.of(source);
		}
	}


	@SuppressWarnings("serial")
	private static class GenericTypeDescriptor extends TypeDescriptor {

		public GenericTypeDescriptor(TypeDescriptor typeDescriptor) {
			super(typeDescriptor.getResolvableType().getGeneric(), null, typeDescriptor.getAnnotations());
		}
	}

}
