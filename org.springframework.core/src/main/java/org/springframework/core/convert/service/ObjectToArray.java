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
 * Special converter that converts an object to an single-element array. Mainly used internally by
 * {@link ConversionService} implementations.
 * 
 * @author Keith Donald
 */
@SuppressWarnings("unchecked")
class ObjectToArray implements SuperConverter {

	private ConversionService conversionService;

	private ConversionExecutor elementConverter;

	/**
	 * Creates a new object to array converter.
	 * @param conversionService the conversion service to resolve the converter to use to convert the object added to
	 * the target array.
	 */
	public ObjectToArray(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Creates a new object to array converter.
	 * @param elementConverter a specific converter to use to convert the object added to the target array.
	 */
	public ObjectToArray(ConversionExecutor elementConverter) {
		this.elementConverter = elementConverter;
	}

	public Object convert(Object source, Class targetClass) throws Exception {
		Class componentType = targetClass.getComponentType();
		Object array = Array.newInstance(componentType, 1);
		ConversionExecutor converter;
		if (elementConverter != null) {
			converter = elementConverter;
		} else {
			converter = conversionService.getConversionExecutor(source.getClass(), componentType);
		}
		Array.set(array, 0, converter.execute(source));
		return array;
	}

}