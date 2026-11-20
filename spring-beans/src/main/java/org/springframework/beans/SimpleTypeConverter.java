/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

/**
 * Simple implementation of the {@link TypeConverter} interface that does not operate on
 * a specific target object. This is an alternative to using a full-blown BeanWrapperImpl
 * instance for arbitrary type conversion needs, while using the very same conversion
 * algorithm (including delegation to {@link java.beans.PropertyEditor} and
 * {@link org.springframework.core.convert.ConversionService}) underneath.
 *
 * <p><b>Note:</b> Due to its reliance on {@link java.beans.PropertyEditor PropertyEditors},
 * SimpleTypeConverter is <em>not</em> thread-safe. Use a separate instance for each thread.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see BeanWrapperImpl
 */
public class SimpleTypeConverter extends TypeConverterSupport {

	public SimpleTypeConverter() {
		this.typeConverterDelegate = new TypeConverterDelegate(this);
		registerDefaultEditors();
	}

}
