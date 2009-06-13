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
package org.springframework.ui;

import java.util.Map;

import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.binding.UserValues;
import org.springframework.ui.binding.support.WebBinder;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.MessageContext;
import org.springframework.ui.validation.ValidationResults;
import org.springframework.ui.validation.Validator;

public class WebBindAndValidateLifecycle {

	private final WebBinder binder;

	@SuppressWarnings("unused")
	private final MessageContext messageContext;

	private ValidationDecider validationDecider = ValidationDecider.ALWAYS_VALIDATE;

	private Validator validator;

	public WebBindAndValidateLifecycle(Object model, MessageContext messageContext) {
		this.binder = new WebBinder(model);
		this.messageContext = messageContext;
	}

	public void execute(Map<String, ? extends Object> userMap) {
		UserValues values = binder.createUserValues(userMap);
		BindingResults bindingResults = binder.bind(values);
		if (validationDecider.shouldValidateAfter(bindingResults)) {
			ValidationResults validationResults = validator.validate(binder.getModel(), bindingResults.successes()
					.properties());
		}
		// TODO translate binding and validation results into messages
		MessageBuilder builder = new MessageBuilder();
		for (BindingResult result : bindingResults.failures()) {
			builder.
				code(modelPropertyError(result)).
				code(propertyError(result)).
				code(typeError(result)).
				code(error(result)).
				//argContextFactory(createContextFactory(bindingResult)).
				// TODO arg names
				// TODO el support including ability to setup evaluation context
				//resolvableArg("label", getModelProperty(result)).
				//arg("value", result.getUserValue()).
				//arg("binding", binder.getBinding(result.getProperty())).
				//args(result.getErrorArguments()).
				build();
		}
		// TODO expose property Binding in EL context for property error message resolution?
	}
	
	private String modelPropertyError(BindingResult result) {
		return getModelProperty(result) + "." + result.getErrorCode();
	}

	private String propertyError(BindingResult result) {
		return result.getProperty() + "." + result.getErrorCode();
	}

	private String typeError(BindingResult result) { 
		return binder.getBinding(result.getProperty()).getType().getName() + "." + result.getErrorCode();
	}
	
	private String error(BindingResult result) {
		return result.getErrorCode();
	}
	
	private String getModelProperty(BindingResult result) {
		return getModel() + "." + result.getProperty();
	}
	
	private String getModel() {
		// TODO would be nice if model name was module.ClassName by default where module is subpackage of app base package
		return binder.getModel().getClass().getName();
	}

	interface ValidationDecider {

		boolean shouldValidateAfter(BindingResults results);

		static final ValidationDecider ALWAYS_VALIDATE = new ValidationDecider() {
			public boolean shouldValidateAfter(BindingResults results) {
				return true;
			}
		};
	}
}
