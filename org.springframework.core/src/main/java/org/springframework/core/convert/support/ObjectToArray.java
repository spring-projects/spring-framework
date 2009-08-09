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

import java.lang.reflect.Array;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts an object to a single-element array.
 * TODO - this class throws cryptic exception if it can't convert to required target array element type.
 *
 * @author Keith Donald
 * @since 3.0
 */
class ObjectToArray implements ConversionExecutor {

	private final TypeDescriptor targetArrayType;

	private final ConversionExecutor elementConverter;
	

	public ObjectToArray(TypeDescriptor sourceObjectType, TypeDescriptor targetArrayType,
			GenericConversionService conversionService) {

		this.targetArrayType = targetArrayType;
		this.elementConverter = conversionService.getConversionExecutor(
				sourceObjectType.getType(), TypeDescriptor.valueOf(targetArrayType.getElementType()));
	}


	public Object execute(Object source) throws ConversionFailedException {
		Object array = Array.newInstance(targetArrayType.getElementType(), 1);
		Object element = elementConverter.execute(source);
		Array.set(array, 0, element);		
		return array;
	}

}