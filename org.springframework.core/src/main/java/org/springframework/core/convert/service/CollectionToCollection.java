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
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.SuperConverter;

/**
 * A converter that can convert from one collection type to another.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
class CollectionToCollection implements SuperConverter<Collection, Collection> {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;

	/**
	 * Creates a new collection-to-collection converter
	 * @param conversionService the conversion service to use to convert collection elements to add to the target
	 * collection
	 */
	public CollectionToCollection(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Creates a new collection-to-collection converter
	 * @param elementConverter a specific converter to use to convert collection elements added to the target collection
	 */
	public CollectionToCollection(ConversionExecutor elementConverter) {
		this.elementConverter = elementConverter;
	}

	public Collection convert(Collection source, Class targetClass) throws Exception {
		Class implClass = CollectionConversionUtils.getImpl(targetClass);
		Collection targetCollection = (Collection) implClass.getConstructor((Class[]) null)
				.newInstance((Object[]) null);
		ConversionExecutor elementConverter = getElementConverter(source, targetClass);
		Collection sourceCollection = (Collection) source;
		Iterator it = sourceCollection.iterator();
		while (it.hasNext()) {
			Object value = it.next();
			if (elementConverter != null) {
				value = elementConverter.execute(value);
			}
			targetCollection.add(value);
		}
		return targetCollection;
	}

	private ConversionExecutor getElementConverter(Object source, Class targetClass) {
		if (elementConverter != null) {
			return elementConverter;
		} else {
			Class elementType = GenericCollectionTypeResolver.getCollectionType(targetClass);
			if (elementType != null) {
				Class componentType = source.getClass().getComponentType();
				return conversionService.getConversionExecutor(componentType, elementType);
			}
			return null;
		}
	}

}