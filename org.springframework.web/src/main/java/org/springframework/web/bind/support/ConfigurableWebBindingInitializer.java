/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.web.bind.support;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.validation.BindingErrorProcessor;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.WebRequest;

/**
 * Convenient {@link WebBindingInitializer} for declarative configuration
 * in a Spring application context. Allows for reusing pre-configured
 * initializers with multiple controller/handlers.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see #setDirectFieldAccess
 * @see #setMessageCodesResolver
 * @see #setBindingErrorProcessor
 * @see #setPropertyEditorRegistrar
 */
public class ConfigurableWebBindingInitializer implements WebBindingInitializer {

	private boolean directFieldAccess = false;

	private MessageCodesResolver messageCodesResolver;

	private BindingErrorProcessor bindingErrorProcessor;

	private PropertyEditorRegistrar[] propertyEditorRegistrars;


	/**
	 * Set whether to use direct field access instead of bean property access.
	 * <p>Default is <code>false</code>, using bean property access.
	 * Switch this to <code>true</code> for enforcing direct field access.
	 */
	public final void setDirectFieldAccess(boolean directFieldAccess) {
		this.directFieldAccess = directFieldAccess;
	}

	/**
	 * Set the strategy to use for resolving errors into message codes.
	 * Applies the given strategy to all data binders used by this controller.
	 * <p>Default is <code>null</code>, i.e. using the default strategy of
	 * the data binder.
	 * @see org.springframework.validation.DataBinder#setMessageCodesResolver
	 */
	public final void setMessageCodesResolver(MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * Return the strategy to use for resolving errors into message codes.
	 */
	public final MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * Set the strategy to use for processing binding errors, that is,
	 * required field errors and <code>PropertyAccessException</code>s.
	 * <p>Default is <code>null</code>, that is, using the default strategy
	 * of the data binder.
	 * @see org.springframework.validation.DataBinder#setBindingErrorProcessor
	 */
	public final void setBindingErrorProcessor(BindingErrorProcessor bindingErrorProcessor) {
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * Return the strategy to use for processing binding errors.
	 */
	public final BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * Specify a single PropertyEditorRegistrar to be applied
	 * to every DataBinder that this controller uses.
	 */
	public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrars = new PropertyEditorRegistrar[] {propertyEditorRegistrar};
	}

	/**
	 * Specify multiple PropertyEditorRegistrars to be applied
	 * to every DataBinder that this controller uses.
	 */
	public final void setPropertyEditorRegistrars(PropertyEditorRegistrar[] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * Return the PropertyEditorRegistrars to be applied
	 * to every DataBinder that this controller uses.
	 */
	public final PropertyEditorRegistrar[] getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}


	public void initBinder(WebDataBinder binder, WebRequest request) {
		if (this.directFieldAccess) {
			binder.initDirectFieldAccess();
		}
		if (this.messageCodesResolver != null) {
			binder.setMessageCodesResolver(this.messageCodesResolver);
		}
		if (this.bindingErrorProcessor != null) {
			binder.setBindingErrorProcessor(this.bindingErrorProcessor);
		}
		if (this.propertyEditorRegistrars != null) {
			for (int i = 0; i < this.propertyEditorRegistrars.length; i++) {
				this.propertyEditorRegistrars[i].registerCustomEditors(binder);
			}
		}
	}

}
