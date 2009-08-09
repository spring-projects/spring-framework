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

import org.springframework.core.convert.TypeDescriptor;

/**
 * Converts a comma-delimited string to an array.
 *
 * @author Keith Donald
 * @since 3.0
 */
@SuppressWarnings("unchecked")
class StringToArray implements ConversionExecutor {

	private ArrayToArray converter;
	
	public StringToArray(TypeDescriptor sourceType, TypeDescriptor targetType, GenericConversionService conversionService) {
		converter = new ArrayToArray(TypeDescriptor.valueOf(String[].class), targetType, conversionService);
	}
	
	public Object execute(Object source) {
		String string = (String) source;
		String[] fields = string.split(",");
		return converter.execute(fields);
	}
}
