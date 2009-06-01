package org.springframework.ui.binding;

import java.util.Map;

public interface BindingFailure {
	
	String getCode();

	String getSeverity();
	
	// TODO - where does arg formatting occur
	Map<String, Object> getArgs();
	
	Map<String, String> getDetails();
}
