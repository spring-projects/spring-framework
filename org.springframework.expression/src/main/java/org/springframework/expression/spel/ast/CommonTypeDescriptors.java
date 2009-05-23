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

import org.springframework.core.convert.TypeDescriptor;

/**
 * @author Andy Clement
 * @since 3.0
 */
public interface CommonTypeDescriptors {
    // TODO push into TypeDescriptor?
	static TypeDescriptor BOOLEAN_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Boolean.class);
	static TypeDescriptor INTEGER_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Integer.class);
	static TypeDescriptor CHARACTER_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Character.class);
	static TypeDescriptor LONG_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Long.class);
	static TypeDescriptor SHORT_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Short.class);
	static TypeDescriptor BYTE_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Byte.class);
	static TypeDescriptor FLOAT_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Float.class);
	static TypeDescriptor DOUBLE_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Double.class);
	static TypeDescriptor STRING_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(String.class);
	static TypeDescriptor CLASS_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Class.class);
	static TypeDescriptor OBJECT_TYPE_DESCRIPTOR = TypeDescriptor.valueOf(Object.class);
	
}
