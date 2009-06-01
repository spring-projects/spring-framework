package org.springframework.ui.binding;

public interface Binding {

	// single-value properties
	
	String getFormattedValue();
	
	void setValue(String formatted);

	String format(Object possibleValue);

	// multi-value properties
	
	boolean isCollection();

	String[] getFormattedValues();

	void setValues(String[] formattedValues);

	// validation metadata
	
	boolean isRequired();
	
	Messages getMessages();	
	
}