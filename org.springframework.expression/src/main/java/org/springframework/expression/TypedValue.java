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

package org.springframework.expression;

import org.springframework.core.convert.TypeDescriptor;

/**
 * Encapsulates an object and a type descriptor that describes it.
 * The type descriptor can hold generic information that would
 * not be accessible through a simple getClass() call on the object.
 *
 * @author Andy Clement
 * @since 3.0
 */
public class TypedValue {

	public static final TypedValue NULL_TYPED_VALUE = new TypedValue(null, TypeDescriptor.NULL);


	private final Object value;

	private final TypeDescriptor typeDescriptor;
	

	/**
	 * Create a TypedValue for a simple object. The type descriptor is inferred
	 * from the object, so no generic information is preserved.
	 * @param value the object value
	 */
	public TypedValue(Object value) {
		this.value = value;
		this.typeDescriptor = TypeDescriptor.forObject(value);
	}
	
	/**
	 * Create a TypedValue for a particular value with a particular type descriptor.
	 * @param value the object value
	 * @param typeDescriptor a type descriptor describing the type of the value
	 */
	public TypedValue(Object value, TypeDescriptor typeDescriptor) {
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}
	

	public Object getValue() {
		return this.value;
	}
	
	public TypeDescriptor getTypeDescriptor() {
		return this.typeDescriptor;
	}


	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("TypedValue: ").append(this.value).append(" of type ").append(this.typeDescriptor.asString());
		return str.toString();
	}

}
