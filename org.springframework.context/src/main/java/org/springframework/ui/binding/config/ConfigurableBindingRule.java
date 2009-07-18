package org.springframework.ui.binding.config;

import org.springframework.ui.format.Formatter;

public class ConfigurableBindingRule implements BindingRuleConfiguration, BindingRule {
	
	private String propertyPath;
	
	private Formatter<?> formatter;
	
	private boolean required;

	private boolean collectionBinding;
	
	private Formatter<?> elementFormatter;
	
	public ConfigurableBindingRule(String propertyPath) {
		this.propertyPath = propertyPath;
	}

	// implementing BindingRuleConfiguration
	
	public BindingRuleConfiguration formatWith(Formatter<?> formatter) {
		this.formatter = formatter;
		return this;
	}

	public BindingRuleConfiguration required() {
		this.required = true;
		return this;
	}
	
	public BindingRuleConfiguration formatElementsWith(Formatter<?> formatter) {
		this.elementFormatter = formatter;
		return this;
	}

	// implementing BindingRule
	
	public String getPropertyPath() {
		return propertyPath;
	}

	public Formatter<?> getFormatter() {
		return formatter;
	}

	public boolean isRequired() {
		return required;
	}	
	
	public boolean isCollectionBinding() {
		return collectionBinding;
	}
	
	public void markCollectionBinding() {
		this.collectionBinding = true;
	}

	public Formatter<?> getValueFormatter() {
		return elementFormatter;
	}

	public Formatter<?> getKeyFormatter() {
		return null;
	}	

}