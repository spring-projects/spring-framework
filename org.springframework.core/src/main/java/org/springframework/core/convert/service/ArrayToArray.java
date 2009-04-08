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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Special one-way converter that converts from a source array to a target array. Supports type conversion of the
 * individual array elements; for example, the ability to convert a String[] to an Integer[]. Mainly used internally by
 * {@link ConversionService} implementations.
 * 
 * @author Keith Donald
 */
class ArrayToArray extends AbstractCollectionConverter {

	public ArrayToArray(TypeDescriptor sourceArrayType, TypeDescriptor targetArrayType, GenericConversionService conversionService) {
		super(sourceArrayType, targetArrayType, conversionService);
	}

	@Override
	public Object doExecute(Object sourceArray) throws Exception {
		int length = Array.getLength(sourceArray);
		Object targetArray = Array.newInstance(getTargetElementType(), length);
		for (int i = 0; i < length; i++) {
			Object value = Array.get(sourceArray, i);
			Array.set(targetArray, i, getElementConverter().execute(value));
		}
		return targetArray;
	}

}
