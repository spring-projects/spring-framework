package org.springframework.ui.binding;

public interface Binding {

	// single-value properties
	
	String getValue();
	
	void setValue(String formatted);

	String format(Object selectableValue);

	// multi-value properties
	
	boolean isCollection();

	String[] getValues();

	void setValues(String[] formattedValues);

	// validation metadata
	
	BindingFailures getFailures();	
	
}