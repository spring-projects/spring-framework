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
package org.springframework.model.binder.support;

import org.springframework.context.MessageSource;
import org.springframework.core.style.StylerUtils;
import org.springframework.model.alert.Alert;
import org.springframework.model.alert.Severity;
import org.springframework.model.binder.BindingResult;
import org.springframework.model.message.DefaultMessageFactory;
import org.springframework.model.message.MessageBuilder;
import org.springframework.model.message.ResolvableArgument;

public class FieldNotEditableResult implements BindingResult {

	private String fieldName;

	private Object submittedValue;

	private MessageSource messageSource;

	public FieldNotEditableResult(String fieldName, Object submittedValue, MessageSource messageSource) {
		this.fieldName = fieldName;
		this.submittedValue = submittedValue;
		this.messageSource = messageSource;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Object getSubmittedValue() {
		return submittedValue;
	}

	public boolean isFailure() {
		return true;
	}

	public Alert getAlert() {
		return new Alert() {
			public String getCode() {
				return "fieldNotEditable";
			}

			public Severity getSeverity() {
				return Severity.WARNING;
			}

			public String getMessage() {
				MessageBuilder builder = new MessageBuilder(messageSource);
				builder.code(getCode());
				builder.arg("label", new ResolvableArgument(fieldName));
				builder.arg("value", submittedValue);
				builder.defaultMessage(new DefaultMessageFactory() {
					public String createDefaultMessage() {
						return "Failed to bind submitted value " + StylerUtils.style(submittedValue) + "; field '" + fieldName + "' is not editable";
					}
				});
				return builder.build();
			}
		};
	}

	public String toString() {
		return getAlert().toString();
	}

}