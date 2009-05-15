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
package org.springframework.expression.spel.ast;

import org.springframework.core.convert.ConversionPoint;

/**
 * @author Andy Clement
 * @since 3.0
 */
public interface CommonTypeDescriptors {
    // TODO push into TypeDescriptor?
	static ConversionPoint BOOLEAN_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Boolean.class);
	static ConversionPoint INTEGER_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Integer.class);
	static ConversionPoint CHARACTER_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Character.class);
	static ConversionPoint LONG_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Long.class);
	static ConversionPoint SHORT_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Short.class);
	static ConversionPoint BYTE_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Byte.class);
	static ConversionPoint FLOAT_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Float.class);
	static ConversionPoint DOUBLE_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Double.class);
	static ConversionPoint STRING_TYPE_DESCRIPTOR = ConversionPoint.valueOf(String.class);
	static ConversionPoint CLASS_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Class.class);
	static ConversionPoint OBJECT_TYPE_DESCRIPTOR = ConversionPoint.valueOf(Object.class);
	
}
