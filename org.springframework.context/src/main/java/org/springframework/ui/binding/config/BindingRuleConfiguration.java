/*
 * Copyright 2004-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ui.binding.config;

import org.springframework.ui.format.Formatter;

/**
 * A fluent interface for configuring a newly added binding rule.
 * @author Keith Donald
 */
public interface BindingRuleConfiguration {
	
	/**
	 * Set the Formatter to use to format bound property values.
	 * If a collection property, this formatter is used to format the Collection as a String.
	 * Default is null.
	 */
	BindingRuleConfiguration formatWith(Formatter<?> formatter);

	/**
	 * If a indexable map property, set the Formatter to use to format map key indexes.
	 * Default is null.
	 */
	BindingRuleConfiguration formatKeysWith(Formatter<?> formatter);

	/**
	 * If an indexable list or map property, set the Formatter to use to format indexed elements.
	 * Default is null.
	 */
	BindingRuleConfiguration formatElementsWith(Formatter<?> formatter);
	
	/**
	 * Mark the binding as read only.
	 * A read-only binding cannot have source values applied and cannot be committed.
	 */
	BindingRuleConfiguration readOnly();

}