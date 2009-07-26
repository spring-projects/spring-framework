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
package org.springframework.model.binder.support;

import java.util.Map;

import org.springframework.context.MessageSource;
import org.springframework.model.binder.Binder;
import org.springframework.model.binder.BindingResult;
import org.springframework.model.binder.BindingResults;
import org.springframework.model.binder.MissingFieldException;
import org.springframework.util.Assert;

/**
 * Base Binder implementation that defines common structural elements.
 * Subclasses should parameterized & implement {@link #bind(Map, Object)}.
 * @author Keith Donald
 * @since 3.0
 * @see #setRequiredFields(String[])
 * @see #setMessageSource(MessageSource)
 * @see #createBindTemplate()
 * @see #bind(Map, Object)
 */
public abstract class AbstractBinder<M> implements Binder<M> {

	private BindTemplate bindTemplate;

	private MessageSource messageSource;

	public AbstractBinder() {
		bindTemplate = createBindTemplate();
	}

	/**
	 * Configure the fields for which values must be present in each bind attempt.
	 * @param fieldNames the required field names
	 * @see MissingFieldException
	 */
	public void setRequiredFields(String[] fieldNames) {
		bindTemplate.setRequiredFields(fieldNames);
	}
	
	/**
	 * Configure the MessageSource that resolves localized {@link BindingResult} alert messages.
	 * @param messageSource the message source
	 */
	public void setMessageSource(MessageSource messageSource) {
		Assert.notNull(messageSource, "The MessageSource is required");
		this.messageSource = messageSource;
	}

	public abstract BindingResults bind(Map<String, ? extends Object> fieldValues, M model);

	// subclass hooks
	
	/**
	 * Create the template defining the bulk-binding algorithm.
	 * Subclasses may override to customize the algorithm.
	 */
	protected BindTemplate createBindTemplate() {
		return new BindTemplate();
	}

	/**
	 * The template defining the bulk-binding algorithm.
	 */
	protected BindTemplate getBindTemplate() {
		return bindTemplate;
	}
	
	/**
	 * The configured MessageSource that resolves binding result alert messages.
	 */
	protected MessageSource getMessageSource() {
		return messageSource;
	}
	
}