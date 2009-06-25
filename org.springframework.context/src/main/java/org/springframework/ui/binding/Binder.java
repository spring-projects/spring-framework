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
package org.springframework.ui.binding;

import java.util.Map;

import org.springframework.ui.format.AnnotationFormatterFactory;
import org.springframework.ui.format.Formatter;

/**
 * Binds user-entered values to properties of a model object.
 * @author Keith Donald
 * @since 3.0
 * @see #configureBinding(BindingConfiguration)
 * @see #bind(UserValues)
 */
public interface Binder {

	/**
	 * The model object this binder binds to.
	 * @return the model object
	 */
	Object getModel();

	/**
	 * Configures if this binder is <i>strict</i>; a strict binder requires all bindings to be registered explicitly using {@link #configureBinding(BindingConfiguration)}.
	 * An <i>optimistic</i> binder will implicitly create bindings as required to support {@link #bind(UserValues)} operations.
	 * Default is optimistic.
	 * @param strict strict binder status
	 */
	void setStrict(boolean strict);

	/**
	 * Adds a new binding.
	 * @param configuration the binding configuration
	 * @return the new binding created from the configuration provided
	 */
	Binding configureBinding(BindingConfiguration configuration);

	/**
	 * Adds a Formatter that will format property values of type <code>propertyType</coe>.
	 * @param propertyType the property type
	 * @param formatter the formatter
	 */
	void registerFormatter(Class<?> propertyType, Formatter<?> formatter);

	/**
	 * Adds a AnnotationFormatterFactory that will format values of properties annotated with a specific annotation.
	 * @param factory the annotation formatter factory
	 */
	void registerFormatterFactory(AnnotationFormatterFactory<?, ?> factory);

	/**
	 * Configures the registry of Formatters to query when no explicit Formatter has been registered for a Binding.
	 * Allows Formatters to be applied by property type and by property annotation.
	 * @param registry the formatter registry
	 */
	void setFormatterRegistry(FormatterRegistry registry);

	/**
	 * Returns the binding for the property.
	 * @param property the property path
	 * @return the binding
	 */
	Binding getBinding(String property);

	/**
	 * Bind source values in the map to the properties of the model object.
	 * @param values the source values to bind
	 * @return the results of the binding operation
	 */
	BindingResults bind(Map<String, ? extends Object> sourceValues);

}