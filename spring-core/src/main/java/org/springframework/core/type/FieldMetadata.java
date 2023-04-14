/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.type;

/**
 * Interface that defines abstract access to the annotations of a specific field, in a
 * form that does not require that method's class to be loaded yet.
 *
 * @author Danny Thomas
 */
public interface FieldMetadata extends AnnotatedTypeMetadata {

	/**
	 * Get the name of the underlying field.
	 */
	String getFieldName();

	/**
	 * Get the type of the underlying field's declared type.
	 */
	TypeMetadata getFieldType();

	/**
	 * Determine whether the underlying field is declared as 'static'.
	 */
	boolean isStatic();

	/**
	 * Determine whether the underlying field is marked as 'final'.
	 */
	boolean isFinal();

}
