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
package org.springframework.ui.binding.binder;

import org.springframework.context.MessageSource;
import org.springframework.core.style.StylerUtils;
import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.message.MessageBuilder;
import org.springframework.ui.message.ResolvableArgument;

class FieldNotFoundResult implements BindingResult {

	private String property;

	private Object sourceValue;

	private MessageSource messageSource;
	
	public FieldNotFoundResult(String property, Object sourceValue, MessageSource messageSource) {
		this.property = property;
		this.sourceValue = sourceValue;
		this.messageSource = messageSource;
	}

	public String getFieldName() {
		return property;
	}

	public Object getSubmittedValue() {
		return sourceValue;
	}

	public boolean isFailure() {
		return true;
	}

	public Alert getAlert() {
		return new Alert() {
			public String getCode() {
				return "propertyNotFound";
			}

			public Severity getSeverity() {
				return Severity.WARNING;
			}

			public String getMessage() {
				MessageBuilder builder = new MessageBuilder(messageSource);
				builder.code("bindSuccess");
				builder.arg("label", new ResolvableArgument(property));
				builder.arg("value", sourceValue);
				// TODO lazily create default message
				builder.defaultMessage("Successfully bound user value " + StylerUtils.style(sourceValue)
						+ " to property '" + property + "'");
				return builder.build();
			}
		};
	}

	public String toString() {
		return getAlert().toString();
	}

}