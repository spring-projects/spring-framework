/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.style;

/**
 * A strategy interface for pretty-printing {@code toString()} methods.
 * Encapsulates the print algorithms; some other object such as a builder
 * should provide the workflow.
 *
 * @author Keith Donald
 * @since 1.2.2
 */
public interface ToStringStyler {

	/**
	 * Style a {@code toString()}'ed object before its fields are styled.
	 * @param buffer the buffer to print to
	 * @param obj the object to style
	 */
	void styleStart(StringBuilder buffer, Object obj);

	/**
	 * Style a {@code toString()}'ed object after it's fields are styled.
	 * @param buffer the buffer to print to
	 * @param obj the object to style
	 */
	void styleEnd(StringBuilder buffer, Object obj);

	/**
	 * Style a field value as a string.
	 * @param buffer the buffer to print to
	 * @param fieldName the he name of the field
	 * @param value the field value
	 */
	void styleField(StringBuilder buffer, String fieldName, Object value);

	/**
	 * Style the given value.
	 * @param buffer the buffer to print to
	 * @param value the field value
	 */
	void styleValue(StringBuilder buffer, Object value);

	/**
	 * Style the field separator.
	 * @param buffer buffer to print to
	 */
	void styleFieldSeparator(StringBuilder buffer);

}
