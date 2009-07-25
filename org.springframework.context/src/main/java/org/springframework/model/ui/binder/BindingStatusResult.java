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
package org.springframework.model.ui.binder;

import org.springframework.model.alert.Alert;
import org.springframework.model.alert.Severity;
import org.springframework.model.binder.BindingResult;

class BindingStatusResult implements BindingResult {

	private String fieldName;
	
	private Object sourceValue;

	private Alert bindingStatusAlert;
	
	public BindingStatusResult(String fieldName, Object sourceValue, Alert alert) {
		this.fieldName = fieldName;
		this.sourceValue = sourceValue;
		this.bindingStatusAlert = alert;
	}

	public String getFieldName() {
		return fieldName;
	}

	public Object getSubmittedValue() {
		return sourceValue;
	}

	public boolean isFailure() {
		return bindingStatusAlert.getSeverity().compareTo(Severity.INFO) > 1;
	}
	
	public Alert getAlert() {
		return bindingStatusAlert;
	}
	
	public String toString() {
		return getAlert().toString();
	}
	
}