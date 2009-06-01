package org.springframework.ui.binding;

import org.springframework.ui.format.Formatter;

public class BindingConfiguration {
	
	private String property;
	
	private Formatter<?> formatter;
	
	private boolean required;
	
	public BindingConfiguration(String property, Formatter<?> formatter, boolean required) {
		this.property = property;
		this.formatter = formatter;
		this.required = required;
	}

	public String getProperty() {
		return property;
	}

	public Formatter<?> getFormatter() {
		return formatter;
	}

	public boolean isRequired() {
		return required;
	}
	
}
