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

import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.SuperConverter;

/**
 * Special one-way converter that converts from a source array to a target array. Supports type conversion of the
 * individual array elements; for example, the ability to convert a String[] to an Integer[]. Mainly used internally by
 * {@link ConversionService} implementations.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
class ArrayToArray implements SuperConverter {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;

	/**
	 * Creates a new array-to-array converter.
	 * @param conversionService the service to use to lookup conversion executors for individual array elements
	 * dynamically
	 */
	public ArrayToArray(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Creates a new array-to-array converter.
	 * @param elementConverter a specific conversion executor to use to convert elements in the source array to elements
	 * in the target array.
	 */
	public ArrayToArray(ConversionExecutor elementConverter) {
		this.elementConverter = elementConverter;
	}

	public Object convert(Object source, Class targetClass) throws Exception {
		Class sourceComponentType = source.getClass().getComponentType();
		Class targetComponentType = targetClass.getComponentType();
		int length = Array.getLength(source);
		Object targetArray = Array.newInstance(targetComponentType, length);
		ConversionExecutor converter = getElementConverter(sourceComponentType, targetComponentType);
		for (int i = 0; i < length; i++) {
			Object value = Array.get(source, i);
			Array.set(targetArray, i, converter.execute(value));
		}
		return targetArray;
	}

	private ConversionExecutor getElementConverter(Class sourceComponentType, Class targetComponentType) {
		if (elementConverter != null) {
			return elementConverter;
		} else {
			return conversionService.getConversionExecutor(sourceComponentType, targetComponentType);
		}
	}
}
