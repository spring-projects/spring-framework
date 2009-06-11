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
package org.springframework.ui;

import java.util.Map;

import org.springframework.ui.binding.BindingResults;
import org.springframework.ui.binding.UserValues;
import org.springframework.ui.binding.support.WebBinder;
import org.springframework.ui.message.MessageContext;
import org.springframework.ui.validation.Validator;

public class WebBindAndValidateLifecycle {

	private WebBinder binder;
	
	@SuppressWarnings("unused")
	private MessageContext messages;
	
	private ValidationDecider validationDecider = ValidationDecider.ALWAYS_VALIDATE;
	
	private Validator validator;
	
	public WebBindAndValidateLifecycle(Object model, MessageContext messages) {
		this.binder = new WebBinder(model);
	}
	
	public void execute(Map<String, ? extends Object> userMap) {
		UserValues values = binder.createUserValues(userMap);
		BindingResults results = binder.bind(values);
		if (validationDecider.shouldValidateAfter(results)) {
			validator.validate(binder.getModel(), results.successes().properties());
		}
	}
	
	public interface ValidationDecider {
		
		boolean shouldValidateAfter(BindingResults results);

		public static ValidationDecider ALWAYS_VALIDATE = new ValidationDecider() {
			public boolean shouldValidateAfter(BindingResults results) {
				return true;
			}
		};
	}
}
