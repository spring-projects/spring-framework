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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalGenericConverter;

/**
 * Converts from a Map to an array.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class MapToArrayConverter implements ConditionalGenericConverter {

	private final MapToCollectionConverter mapToCollectionHelperConverter;

	private final CollectionToArrayConverter collectionToArrayHelperConverter;

	public MapToArrayConverter(GenericConversionService conversionService) {
		this.mapToCollectionHelperConverter = new MapToCollectionConverter(conversionService);
		this.collectionToArrayHelperConverter = new CollectionToArrayConverter(conversionService);
	}

	public Set<ConvertiblePair> getConvertibleTypes() {
		return Collections.singleton(new ConvertiblePair(Map.class, Object[].class));
	}

	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.mapToCollectionHelperConverter.matches(sourceType, targetType);
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		TypeDescriptor collectionType = TypeDescriptor.collection(List.class, targetType.getElementTypeDescriptor());
		Object collection = this.mapToCollectionHelperConverter.convert(source, sourceType, collectionType);
		return this.collectionToArrayHelperConverter.convert(collection, collectionType, targetType);
	}

}
