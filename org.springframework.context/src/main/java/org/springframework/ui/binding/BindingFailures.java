package org.springframework.ui.binding;

import java.util.List;

public interface BindingFailures {
	
	int getCount();
	
	Severity getMaximumSeverity();
	
	List<BindingFailure> getAll();

	List<BindingFailure> getBySeverity(Severity severity);
	
}
