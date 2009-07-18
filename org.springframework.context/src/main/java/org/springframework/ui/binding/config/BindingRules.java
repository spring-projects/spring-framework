package org.springframework.ui.binding.config;

import java.util.List;

public interface BindingRules extends List<BindingRule> {

	Class<?> getModelType();

}
