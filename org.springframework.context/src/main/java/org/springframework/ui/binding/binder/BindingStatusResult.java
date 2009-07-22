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

import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;

class BindingStatusResult implements BindingResult {

	private String property;
	
	private Object sourceValue;

	private Alert bindingStatusAlert;
	
	public BindingStatusResult(String property, Object sourceValue, Alert alert) {
		this.property = property;
		this.sourceValue = sourceValue;
		this.bindingStatusAlert = alert;
	}

	public String getProperty() {
		return property;
	}

	public Object getSourceValue() {
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