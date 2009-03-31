/*
 * Copyright 2004-2009 the original author or authors.
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
package org.springframework.core.convert.service;

import java.util.Collection;
import java.util.Iterator;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.TypeDescriptor;

/**
 * A converter that can convert from one collection type to another.
 * 
 * @author Keith Donald
 */
class CollectionToCollection extends AbstractCollectionConverter {
	
	public CollectionToCollection(TypeDescriptor sourceCollectionType, TypeDescriptor targetCollectionType,
			GenericConversionService conversionService) {
		super(sourceCollectionType, targetCollectionType, conversionService);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object doExecute(Object source) throws Exception {
		Collection sourceCollection = (Collection) source;
		Class targetCollectionType = getTargetType().getType();
		Class implClass = CollectionConversionUtils.getImpl(targetCollectionType);
		Collection targetCollection = (Collection) implClass.newInstance();
		ConversionExecutor elementConverter = getElementConverter();
		Class elementType;
		if (elementConverter == null) {
			elementType = GenericCollectionTypeResolver.getCollectionType(targetCollectionType);
		} else {
			elementType = null;
		}
		Iterator it = sourceCollection.iterator();
		while (it.hasNext()) {
			Object value = it.next();
			if (elementConverter == null && elementType != null) {
				elementConverter = getConversionService().getElementConverter(value.getClass(), elementType); 			
			}
			value = elementConverter.execute(value);
			targetCollection.add(value);
		}
		return targetCollection;
	}
	
}