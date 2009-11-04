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
 * A generic converter that conditionally executes.
 * Often used when selectively matching custom conversion logic based on the presence of a field or class-level annotation.
 * For example, when converting from a String to a Date field, an implementation might return true if the target field has also been annotated with <code>@DateTimeFormat</code>.
 * @author Keith Donald
 * @since 3.0
 */
public interface ConditionalGenericConverter extends GenericConverter {

	/**
	 * Should the conversion between <code>sourceFieldType</code> and <code>targetFieldType</code> be performed?
	 * @param sourceFieldType the type descriptor of the field we are converting from
	 * @param targetFieldType the type descriptor of the field we are converting to
	 * @return true if conversion should be performed, false otherwise
	 */
	boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);
	
}