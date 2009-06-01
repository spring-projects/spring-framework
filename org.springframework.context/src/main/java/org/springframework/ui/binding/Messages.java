package org.springframework.ui.binding;

import java.util.List;

public interface Messages {
	
	int getCount();
	
	Severity getMaximumSeverity();
	
	List<Message> getAll();

	List<Message> getBySeverity(Severity severity);
	
}
