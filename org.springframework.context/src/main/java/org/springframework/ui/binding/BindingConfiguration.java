package org.springframework.ui.binding;

import org.springframework.ui.format.Formatter;

public class BindingConfiguration {
	
	private String property;
	
	private Formatter<?> formatter;
	
	public BindingConfiguration(String property, Formatter<?> formatter) {
		this.property = property;
		this.formatter = formatter;
	}

	public String getProperty() {
		return property;
	}

	public Formatter<?> getFormatter() {
		return formatter;
	}

}
