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

import org.springframework.core.convert.ConversionContext;

/**
 * @author Andy Clement
 * @since 3.0
 */
public interface CommonTypeDescriptors {
    // TODO push into TypeDescriptor?
	static ConversionContext BOOLEAN_TYPE_DESCRIPTOR = ConversionContext.valueOf(Boolean.class);
	static ConversionContext INTEGER_TYPE_DESCRIPTOR = ConversionContext.valueOf(Integer.class);
	static ConversionContext CHARACTER_TYPE_DESCRIPTOR = ConversionContext.valueOf(Character.class);
	static ConversionContext LONG_TYPE_DESCRIPTOR = ConversionContext.valueOf(Long.class);
	static ConversionContext SHORT_TYPE_DESCRIPTOR = ConversionContext.valueOf(Short.class);
	static ConversionContext BYTE_TYPE_DESCRIPTOR = ConversionContext.valueOf(Byte.class);
	static ConversionContext FLOAT_TYPE_DESCRIPTOR = ConversionContext.valueOf(Float.class);
	static ConversionContext DOUBLE_TYPE_DESCRIPTOR = ConversionContext.valueOf(Double.class);
	static ConversionContext STRING_TYPE_DESCRIPTOR = ConversionContext.valueOf(String.class);
	static ConversionContext CLASS_TYPE_DESCRIPTOR = ConversionContext.valueOf(Class.class);
	static ConversionContext OBJECT_TYPE_DESCRIPTOR = ConversionContext.valueOf(Object.class);
	
}
