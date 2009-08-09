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

import java.util.Collection;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts an object to a single-element collection.
 *
 * @author Keith Donald
 * @since 3.0
 */
class ObjectToCollection implements ConversionExecutor {

	private final TypeDescriptor targetCollectionType;

	private final ConversionExecutor elementConverter;
	

	public ObjectToCollection(TypeDescriptor sourceObjectType, TypeDescriptor targetCollectionType,
			GenericConversionService typeConverter) {

		this.targetCollectionType = targetCollectionType;
		Class<?> elementType = targetCollectionType.getElementType();
		if (elementType != null) {
			this.elementConverter = typeConverter.getConversionExecutor(sourceObjectType.getType(), TypeDescriptor.valueOf(elementType));
		}
		else {
			this.elementConverter = NoOpConversionExecutor.INSTANCE;
		}
	}

	@SuppressWarnings("unchecked")
	public Object execute(Object source) throws ConversionFailedException {
		Collection collection = CollectionFactory.createCollection(this.targetCollectionType.getType(), 1);
		collection.add(this.elementConverter.execute(source));
		return collection;
	}

}
