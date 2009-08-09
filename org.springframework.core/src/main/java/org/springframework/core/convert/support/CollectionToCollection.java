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
import org.springframework.core.convert.TypeDescriptor;

/**
 * A converter that can convert from one collection type to another.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class CollectionToCollection extends AbstractCollectionConverter {
	
	public CollectionToCollection(TypeDescriptor sourceCollectionType, TypeDescriptor targetCollectionType,
			GenericConversionService conversionService) {
		super(sourceCollectionType, targetCollectionType, conversionService);
	}

	@Override
	protected Object doExecute(Object source) throws Exception {
		Collection sourceCollection = (Collection) source;
		Collection targetCollection = CollectionFactory.createCollection(getTargetCollectionType(), sourceCollection.size());
		ConversionExecutor elementConverter = getElementConverter(sourceCollection);
		for (Object aSourceCollection : sourceCollection) {
			targetCollection.add(elementConverter.execute(aSourceCollection));
		}
		return targetCollection;
	}
	
	private ConversionExecutor getElementConverter(Collection<?> source) {
		ConversionExecutor elementConverter = getElementConverter();
		if (elementConverter == NoOpConversionExecutor.INSTANCE && getTargetElementType() != null) {
			for (Object value : source) {
				if (value != null) {
					elementConverter = getConversionService().getConversionExecutor(
							value.getClass(), TypeDescriptor.valueOf(getTargetElementType()));
					break;
				}
			}
		}
		return elementConverter;
	}
	
}
