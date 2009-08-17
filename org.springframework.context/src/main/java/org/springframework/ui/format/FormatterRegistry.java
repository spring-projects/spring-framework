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
package org.springframework.ui.format;

import java.lang.annotation.Annotation;

import org.springframework.core.convert.TypeDescriptor;

/**
 * A shared registry of Formatters.
 * @author Keith Donald
 * @since 3.0
 */
public interface FormatterRegistry {

	/**
	 * Adds a Formatter to this registry indexed by <T>.
	 * Calling getFormatter(&lt;T&gt;.class) returns <code>formatter</code>.
	 * @param formatter the formatter
	 * @param <T> the type of object the formatter formats
	 */
	<T> void add(Formatter<T> formatter);

	/**
	 * Adds a Formatter to this registry indexed by objectType.
	 * Use this add method when objectType differs from &lt;T&gt;.
	 * Calling getFormatter(objectType) returns a decorator that wraps the targetFormatter.
	 * On format, the decorator first coerses the instance of objectType to &lt;T&gt;, then delegates to <code>targetFormatter</code> to format the value.
	 * On parse, the decorator first delegates to the formatter to parse a &lt;T&gt;, then coerses the parsed value to objectType.
	 * @param objectType the object type
	 * @param targetFormatter the target formatter
	 * @param <T> the type of object the target formatter formats
	 */
	<T> void add(Class<?> objectType, Formatter<T> targetFormatter);

	/**
	 * Adds a AnnotationFormatterFactory that will format values of properties annotated with a specific annotation.
	 * @param factory the annotation formatter factory
	 */
	<A extends Annotation, T> void add(AnnotationFormatterFactory<A, T> factory);

	/**
	 * Get the Formatter for the type.
	 * @return the Formatter, or <code>null</code> if none is registered
	 */
	@SuppressWarnings("unchecked")
	Formatter getFormatter(TypeDescriptor type);

}
