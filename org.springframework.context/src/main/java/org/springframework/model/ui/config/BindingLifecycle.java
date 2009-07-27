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

/**
 * A SPI interface that lets you configure a BindingLifecycle for a model, then execute it.
 * Hides details about the source of submitted field values.
 * @author Keith Donald
 * @since 3.0
 * @param <M> the type of model this lifecycle is for
 */
public interface BindingLifecycle<M> {

	/**
	 * Configure the model object to bind to.
	 * Optional operation.
	 * If not called, the model be a new instance of <M> created by invoking it's default constructor.
	 * @param model the model
	 */
	void setModel(M model);

	/**
	 * Execute this binding lifecycle.
	 * The steps are:
	 * <ul>
	 * <li>Get a PresentationModel for model M.</li>
	 * <li>Bind submitted values to the PresentationModel</li>
	 * <li>Validate the PresentationModel</li>.
	 * <li>Commit changes to M if no bind and validation errors occur.</li> 
	 * </ul>
	 * @throws IllegalStateExeption if no model was set and no default constructor was found on M.
	 */
	void execute();

	/**
	 * If executing the lifecycle produced errors.
	 */
	boolean hasErrors();

	/**
	 * Get the model instance this lifecycle executed against.
	 */
	M getModel();

}
