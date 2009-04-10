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

import org.springframework.core.convert.ConversionExecutor;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Special converter that converts from a source array to a target collection. Supports the selection of an
 * "approximate" collection implementation when a target collection interface such as <code>List.class</code> is
 * specified. Supports type conversion of array elements when a parameterized target collection type descriptor is provided.
 * 
 * @author Keith Donald
 */
class ArrayToCollection extends AbstractCollectionConverter {

	public ArrayToCollection(TypeDescriptor sourceArrayType, TypeDescriptor targetCollectionType,
			GenericConversionService conversionService) {
		super(sourceArrayType, targetCollectionType, conversionService);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Object doExecute(Object sourceArray) throws Exception {
		Class implClass = CollectionConversionUtils.getImpl(getTargetCollectionType());
		Collection collection = (Collection) implClass.newInstance();
		int length = Array.getLength(sourceArray);
		ConversionExecutor elementConverter = getElementConverter();
		for (int i = 0; i < length; i++) {
			collection.add(elementConverter.execute(Array.get(sourceArray, i)));
		}
		return collection;
	}

}