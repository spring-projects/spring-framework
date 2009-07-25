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
package org.springframework.model.ui.config;

import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResults;

/**
 * A SPI interface that lets you configure a {@link Binder}, then execute it.
 * Hides details about the source of binder field values.
 * @author Keith Donald
 * @since 3.0
 * @param <M> the type of model to bind to
 */
public interface BinderExecutor<M> {
	
	/**
	 * Configure the model object to bind to.
	 * @param model the model
	 */
	void setModel(M model);
	
	/**
	 * Configure a bindable field.
	 * @param fieldPath the field path, typically a domain object property path on the model object in format &lt;prop&gt;[.nestedProp]
	 * @return a builder for the field model configuration
	 */
	FieldModelConfiguration field(String fieldPath);

	// TODO allow injection of pre-created BindingRules

	/**
	 * Execute the bind operation.
	 * @return the binding results
	 */
	BindingResults bind();

	// TODO return validation results
	/**
	 * Execute the validate operation.
	 */
	void validate();
	
	/**
	 * The model that was bound to.
	 */
	M getModel();
	
}
