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

import java.lang.reflect.Constructor;
import java.util.Collection;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.SuperConverter;

/**
 * Special two-way converter that converts an object to an single-element collection. Supports type conversion of the
 * individual element with parameterized collection implementations.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
class ObjectToCollection implements SuperConverter<Object, Collection> {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;

	/**
	 * Creates a new object to collection converter
	 * @param conversionService the conversion service to lookup the converter to use to convert an object when adding
	 * it to a target collection
	 */
	public ObjectToCollection(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Creates a new object to collection converter
	 * @param elementConverter a specific converter to execute on an object when adding it to a target collection
	 */
	public ObjectToCollection(ConversionExecutor elementConverter) {
		this.elementConverter = elementConverter;
	}

	public Collection convert(Object source, Class targetClass) throws Exception {
		Class implClass = CollectionConversionUtils.getImpl(targetClass);
		Constructor constructor = implClass.getConstructor((Class[]) null);
		Collection collection = (Collection) constructor.newInstance((Object[]) null);
		ConversionExecutor converter = getElementConverter(source, targetClass);
		Object value;
		if (converter != null) {
			value = converter.execute(source);
		} else {
			value = source;
		}
		collection.add(value);
		return collection;
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