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

import org.springframework.ui.alert.AlertContext;
import org.springframework.ui.binding.Binder;
import org.springframework.ui.binding.BindingResult;
import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.validation.Validator;

/**
 * Implementation of the bind and validate lifecycle for web (HTTP) environments.
 * @author Keith Donald
 * @since 3.0
 */
public class WebBindAndValidateLifecycle {

	private final Binder binder;

	private final AlertContext alertContext;

	private ValidationDecider validationDecider = ValidationDecider.ALWAYS_VALIDATE;

	private Validator validator;

	public WebBindAndValidateLifecycle(Binder binder, AlertContext alertContext) {
		this.binder = binder;
		this.alertContext = alertContext;
	}

	public void execute(Map<String, ? extends Object> sourceValues) {
		BindingResults bindingResults = binder.bind(sourceValues);
		if (validator != null && validationDecider.shouldValidateAfter(bindingResults)) {
			// TODO get validation results
			validator.validate(binder.getModel(), bindingResults.successes().properties());
		}
		for (BindingResult result : bindingResults.failures()) {
			alertContext.add(result.getAlert());
		}
		// TODO translate validation results into messages
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
