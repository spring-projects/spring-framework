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

import org.springframework.core.convert.BindingPoint;

/**
 * @author Andy Clement
 * @since 3.0
 */
public interface CommonTypeDescriptors {
    // TODO push into TypeDescriptor?
	static BindingPoint BOOLEAN_TYPE_DESCRIPTOR = BindingPoint.valueOf(Boolean.class);
	static BindingPoint INTEGER_TYPE_DESCRIPTOR = BindingPoint.valueOf(Integer.class);
	static BindingPoint CHARACTER_TYPE_DESCRIPTOR = BindingPoint.valueOf(Character.class);
	static BindingPoint LONG_TYPE_DESCRIPTOR = BindingPoint.valueOf(Long.class);
	static BindingPoint SHORT_TYPE_DESCRIPTOR = BindingPoint.valueOf(Short.class);
	static BindingPoint BYTE_TYPE_DESCRIPTOR = BindingPoint.valueOf(Byte.class);
	static BindingPoint FLOAT_TYPE_DESCRIPTOR = BindingPoint.valueOf(Float.class);
	static BindingPoint DOUBLE_TYPE_DESCRIPTOR = BindingPoint.valueOf(Double.class);
	static BindingPoint STRING_TYPE_DESCRIPTOR = BindingPoint.valueOf(String.class);
	static BindingPoint CLASS_TYPE_DESCRIPTOR = BindingPoint.valueOf(Class.class);
	static BindingPoint OBJECT_TYPE_DESCRIPTOR = BindingPoint.valueOf(Object.class);
	
}
