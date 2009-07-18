package org.springframework.ui.binding.config;

import org.springframework.ui.format.Formatter;

public interface BindingRule {
	
	String getPropertyPath();
	
	Formatter<?> getFormatter();
	
	boolean isRequired();
	
	boolean isCollectionBinding();

	Formatter<?> getKeyFormatter();

	Formatter<?> getValueFormatter();
	
}