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
import java.util.Collection;
import java.util.Iterator;

import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.SuperConverter;

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
class CollectionToArray implements SuperConverter {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;

	/**
	 * Creates a new array to collection converter.
	 * @param conversionService the conversion service to use to lookup the converter to apply to array elements added
	 * to the target collection
	 */
	public CollectionToArray(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Creates a new array to collection converter.
	 * @param elementConverter A specific converter to use on array elements when adding them to the target collection
	 */
	public CollectionToArray(ConversionExecutor elementConverter) {
		this.elementConverter = elementConverter;
	}

	public Object convert(Object source, Class targetClass) throws Exception {
		Collection collection = (Collection) source;
		Object array = Array.newInstance(targetClass.getComponentType(), collection.size());
		int i = 0;
		for (Iterator it = collection.iterator(); it.hasNext(); i++) {
			Object value = it.next();
			if (value != null) {
				ConversionExecutor converter;
				if (elementConverter != null) {
					converter = elementConverter;
				} else {
					converter = conversionService.getConversionExecutor(value.getClass(), targetClass
							.getComponentType());
				}
				value = converter.execute(value);
			}
			Array.set(array, i, value);
		}
		return array;
	}

	public Object convertBack(Object target) throws Exception {
		throw new UnsupportedOperationException("Should never be called");
	}

}