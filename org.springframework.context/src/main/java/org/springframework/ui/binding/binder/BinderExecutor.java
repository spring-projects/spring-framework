package org.springframework.ui.binding.binder;

import org.springframework.ui.binding.config.BindingRuleConfiguration;

public interface BinderExecutor<M> {
	
	void setModel(M model);
	
	BindingRuleConfiguration bindingRule(String property);

	// TODO allow injection of pre-created BindingRules
	
	BindingResults bind();

	// TODO return validation results
	void validate();
	
	M getModel();
	
}
