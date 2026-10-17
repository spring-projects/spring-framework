/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans;

import org.jspecify.annotations.Nullable;

import org.springframework.core.convert.ConversionService;

/**
 * Interface that encapsulates configuration methods for a {@link PropertyAccessor}.
 *
 * <p>Also extends the {@link PropertyEditorRegistry} interface, which defines methods
 * for {@link java.beans.PropertyEditor} management.
 *
 * <p>Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 2.0
 * @see BeanWrapper
 */
public interface ConfigurablePropertyAccessor extends PropertyAccessor, PropertyEditorRegistry, TypeConverter {

	/**
	 * Default maximum nesting depth permitted for a nested property path: {@value}.
	 * <p>This limit guards against deeply nested property paths that could otherwise
	 * drive the recursive resolution of a nested property path to exhaust the current
	 * thread's call stack.
	 * <p><strong>NOTE</strong>: This limit improves diagnostics for the common case
	 * by converting what would otherwise be an opaque {@link StackOverflowError}
	 * into a descriptive {@link InvalidPropertyException}, but it is <em>not</em>
	 * a guaranteed defense against {@code StackOverflowError} under every possible
	 * JVM thread stack size configuration. The amount of stack space consumed per
	 * level of nesting depends on the JVM, its current JIT compilation state, and
	 * the platform.
	 * @since 7.1
	 * @see #setMaxNestedPathDepth(int)
	 */
	int DEFAULT_MAX_NESTED_PATH_DEPTH = 100;


	/**
	 * Specify a {@link ConversionService} to use for converting
	 * property values, as an alternative to JavaBeans PropertyEditors.
	 */
	void setConversionService(@Nullable ConversionService conversionService);

	/**
	 * Return the associated ConversionService, if any.
	 */
	@Nullable ConversionService getConversionService();

	/**
	 * Set whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	void setExtractOldValueForEditor(boolean extractOldValueForEditor);

	/**
	 * Return whether to extract the old property value when applying a
	 * property editor to a new value for a property.
	 */
	boolean isExtractOldValueForEditor();

	/**
	 * Set whether this instance should attempt to "auto-grow" a
	 * nested path that contains a {@code null} value.
	 * <p>If {@code true}, a {@code null} path location will be populated
	 * with a default object value and traversed instead of resulting in a
	 * {@link NullValueInNestedPathException}.
	 * <p>Default is {@code false} on a plain accessor.
	 * @since 4.1
	 */
	void setAutoGrowNestedPaths(boolean autoGrowNestedPaths);

	/**
	 * Return whether "auto-growing" of nested paths has been activated.
	 * @since 4.1
	 */
	boolean isAutoGrowNestedPaths();

	/**
	 * Specify a limit for array and collection auto-growing.
	 * <p>Default is unlimited on a plain accessor.
	 * @since 7.1
	 */
	void setAutoGrowCollectionLimit(int autoGrowCollectionLimit);

	/**
	 * Return the limit for array and collection auto-growing.
	 * @since 7.1
	 */
	int getAutoGrowCollectionLimit();

	/**
	 * Specify the maximum nesting depth permitted for a nested property path.
	 * <p>The nesting depth corresponds to the number of intermediate properties
	 * traversed to reach the final property &mdash; for example,
	 * {@code "address.country.name"} has a nesting depth of 2.
	 * <p>Specify {@code 0} to disable nested property paths altogether, while
	 * still allowing simple, indexed, and mapped property access.
	 * <p>Default is {@link #DEFAULT_MAX_NESTED_PATH_DEPTH}.
	 * @param maxNestedPathDepth the maximum nesting depth; must not be negative
	 * @since 7.1
	 */
	void setMaxNestedPathDepth(int maxNestedPathDepth);

	/**
	 * Return the maximum nesting depth permitted for a nested property path.
	 * @since 7.1
	 */
	int getMaxNestedPathDepth();

}
