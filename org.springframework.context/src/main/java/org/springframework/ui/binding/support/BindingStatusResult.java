/**
 * 
 */
package org.springframework.ui.binding.support;

import org.springframework.ui.alert.Alert;
import org.springframework.ui.alert.Severity;
import org.springframework.ui.binding.BindingResult;

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
	
}