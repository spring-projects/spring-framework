/*
 * Copyright 2002-2013 the original author or authors.
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
 * Encapsulates an object and a type descriptor that describes it. The type descriptor can
 * hold generic information that would not be accessible through a simple
 * {@code getClass()} call on the object.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class TypedValue {

	public static final TypedValue NULL = new TypedValue(null);


	private final Object value;

	private TypeDescriptor typeDescriptor;


	/**
	 * Create a TypedValue for a simple object. The type descriptor is inferred
	 * from the object, so no generic information is preserved.
	 * @param value the object value
	 */
	public TypedValue(Object value) {
		this.value = value;
		this.typeDescriptor = null;  // initialized when/if requested
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
		if (this.typeDescriptor == null) {
			this.typeDescriptor = TypeDescriptor.forObject(this.value);
		}
		return this.typeDescriptor;
	}


	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("TypedValue: '").append(this.value).append("' of [").append(getTypeDescriptor() + "]");
		return str.toString();
	}

}
