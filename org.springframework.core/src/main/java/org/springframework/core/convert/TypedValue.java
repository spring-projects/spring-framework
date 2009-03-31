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
package org.springframework.core.convert;

/**
 * A value with additional context about its type.
 * The additional context provides information about how the value was obtained; for example from a field read or getter return value.
 * This additional context allows access to generic information associated with a field, return value, or method argument.
 * It also allows access to field-level or method-level annotations.
 * All of this context can be utilized when performing a type conversion as part of a data binding routine.
 * 
 * TODO - is this needed?
 * 
 * @author Keith Donald
 */
public class TypedValue {

	/**
	 * The NULL TypedValue object.
	 */
	public static final TypedValue NULL = new TypedValue(null);

	private final Object value;

	private final TypeDescriptor typeDescriptor;

	/**
	 * Creates a new typed value.
	 * @param value the actual value (may be null)
	 */
	public TypedValue(Object value) {
		this.value = value;
		if (this.value != null) {
			typeDescriptor = TypeDescriptor.valueOf(value.getClass());
		} else {
			typeDescriptor = null;
		}
	}

	/**
	 * Creates a new typed value.
	 * @param value the actual value (may be null)
	 * @param typeDescriptor the value type descriptor (may be null)
	 */
	public TypedValue(Object value, TypeDescriptor typeDescriptor) {
		this.value = value;
		this.typeDescriptor = typeDescriptor;
	}

	/**
	 * The actual value. May be null.
	 */
	public Object getValue() {
		return value;
	}

	/**
	 * Provides additional type context about the value. May be null.
	 */
	public TypeDescriptor getTypeDescriptor() {
		return typeDescriptor;
	}

	/**
	 * True if both the actual value and type descriptor are null.
	 */
	public boolean isNull() {
		return value == null && typeDescriptor  == null;
	}

}
