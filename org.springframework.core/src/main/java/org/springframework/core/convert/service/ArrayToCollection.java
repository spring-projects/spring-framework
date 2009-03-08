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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Iterator;

import org.springframework.core.GenericCollectionTypeResolver;
import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.SuperTwoWayConverter;

/**
 * Special converter that converts from a source array to a target collection. Supports the selection of an
 * "approximate" collection implementation when a target collection interface such as <code>List.class</code> is
 * specified. Supports type conversion of array elements when a concrete parameterized collection class is provided,
 * such as <code>IntegerList<Integer>.class</code>.
 * 
 * Note that type erasure prevents arbitrary access to generic collection element type information at runtime,
 * preventing the ability to convert elements for collections declared as properties.
 * 
 * Mainly used internally by {@link ConversionService} implementations.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
class ArrayToCollection implements SuperTwoWayConverter {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;

	/**
	 * Creates a new array to collection converter.
	 * @param conversionService the conversion service to use to lookup the converter to apply to array elements added
	 * to the target collection
	 */
	public ArrayToCollection(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Creates a new array to collection converter.
	 * @param elementConverter A specific converter to use on array elements when adding them to the target collection
	 */
	public ArrayToCollection(ConversionExecutor elementConverter) {
		this.elementConverter = elementConverter;
	}

	public Object convert(Object source, Class targetClass) throws Exception {
		Class implClass = CollectionConversionUtils.getImpl(targetClass);
		Constructor constructor = implClass.getConstructor((Class[]) null);
		Collection collection = (Collection) constructor.newInstance((Object[]) null);
		ConversionExecutor converter = getArrayElementConverter(source, targetClass);
		int length = Array.getLength(source);
		for (int i = 0; i < length; i++) {
			Object value = Array.get(source, i);
			if (converter != null) {
				value = converter.execute(value);
			}
			collection.add(value);
		}
		return collection;
	}

	public Object convertBack(Object target, Class sourceClass) throws Exception {
		Collection collection = (Collection) target;
		Class elementType = sourceClass.getComponentType();
		Object array = Array.newInstance(elementType, collection.size());
		int i = 0;
		for (Iterator it = collection.iterator(); it.hasNext(); i++) {
			Object value = it.next();
			if (value != null) {
				ConversionExecutor converter;
				if (elementConverter != null) {
					converter = elementConverter;
				} else {
					converter = conversionService.getConversionExecutor(value.getClass(), elementType);
				}
				value = converter.execute(value);
			}
			Array.set(array, i, value);
		}
		return array;
	}

	private ConversionExecutor getArrayElementConverter(Object source, Class targetClass) {
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