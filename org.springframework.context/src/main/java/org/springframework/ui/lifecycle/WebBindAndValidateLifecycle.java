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
package org.springframework.ui.lifecycle;

import java.util.Map;

import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.binding.UserValues;
import org.springframework.ui.binding.support.WebBinder;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.MessageContext;
import org.springframework.ui.message.MessageResolver;
import org.springframework.ui.validation.Validator;

/**
 * Implementation of the bind and validate lifecycle for web (HTTP) environments.
 * @author Keith Donald
 * @since 3.0
 */
public class WebBindAndValidateLifecycle {

	private final WebBinder binder;

	private final MessageContext messageContext;

	private ValidationDecider validationDecider = ValidationDecider.ALWAYS_VALIDATE;

	private Validator validator;

	public WebBindAndValidateLifecycle(Object model, MessageContext messageContext) {
		// TODO allow binder to be configured with bindings from model metadata
		// TODO support @Bound property annotation?
		// TODO support @StrictBinding class-level annotation?
		this.binder = new WebBinder(model);
		this.messageContext = messageContext;
	}

	public void execute(Map<String, ? extends Object> userMap) {
		UserValues values = binder.createUserValues(userMap);
		BindingResults bindingResults = binder.bind(values);
		if (validationDecider.shouldValidateAfter(bindingResults)) {
			// TODO get validation results
			validator.validate(binder.getModel(), bindingResults.successes().properties());
		}
		MessageBuilder builder = new MessageBuilder();
		for (BindingResult result : bindingResults.failures()) {
			MessageResolver message = builder.code(modelPropertyError(result)).code(propertyError(result)).code(
					typeError(result)).code(error(result)).resolvableArg("label", getModelProperty(result)).arg(
					"value", result.getUserValue()).
					// TODO add binding el resolver allowing binding.format to be called
					arg("binding", binder.getBinding(result.getProperty())).
					// TODO allow binding result to contribute additional arguments
					build();
			// TODO should model name be part of element id?
			messageContext.add(message, result.getProperty());
		}
		// TODO translate validation results into messages
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
		// TODO model name should probably be specifiable using class-level annotation
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
