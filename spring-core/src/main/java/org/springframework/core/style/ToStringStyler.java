/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.style;

import org.jspecify.annotations.Nullable;

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
	 * @param fieldName the name of the field
	 * @param value the field value
	 */
	void styleField(StringBuilder buffer, String fieldName, @Nullable Object value);

	/**
	 * Style the given value.
	 * @param buffer the buffer to print to
	 * @param value the field value
	 */
	void styleValue(StringBuilder buffer, Object value);

	/**
	 * Style the field separator.
	 * @param buffer the buffer to print to
	 */
	void styleFieldSeparator(StringBuilder buffer);

}
