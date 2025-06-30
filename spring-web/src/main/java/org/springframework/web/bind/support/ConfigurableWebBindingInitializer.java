/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.bind.support;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.core.convert.ConversionService;
import org.springframework.validation.BindingErrorProcessor;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.WebDataBinder;

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
 * @see #setValidator(Validator)
 * @see #setConversionService(ConversionService)
 * @see #setPropertyEditorRegistrar
 */
public class ConfigurableWebBindingInitializer implements WebBindingInitializer {

	private boolean autoGrowNestedPaths = true;

	private boolean directFieldAccess = false;

	private @Nullable Boolean declarativeBinding;

	private @Nullable MessageCodesResolver messageCodesResolver;

	private @Nullable BindingErrorProcessor bindingErrorProcessor;

	private @Nullable Validator validator;

	private @Nullable ConversionService conversionService;

	private PropertyEditorRegistrar @Nullable [] propertyEditorRegistrars;


	/**
	 * Set whether a binder should attempt to "auto-grow" a nested path that contains a null value.
	 * <p>If "true", a null path location will be populated with a default object value and traversed
	 * instead of resulting in an exception. This flag also enables auto-growth of collection elements
	 * when accessing an out-of-bounds index.
	 * <p>Default is "true" on a standard DataBinder. Note that this feature is only supported
	 * for bean property access (DataBinder's default mode), not for field access.
	 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
	 * @see org.springframework.validation.DataBinder#setAutoGrowNestedPaths
	 */
	public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
		this.autoGrowNestedPaths = autoGrowNestedPaths;
	}

	/**
	 * Return whether a binder should attempt to "auto-grow" a nested path that contains a null value.
	 */
	public boolean isAutoGrowNestedPaths() {
		return this.autoGrowNestedPaths;
	}

	/**
	 * Set whether to use direct field access instead of bean property access.
	 * <p>Default is {@code false}, using bean property access.
	 * Switch this to {@code true} in order to enforce direct field access.
	 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
	 * @see org.springframework.validation.DataBinder#initBeanPropertyAccess()
	 */
	public final void setDirectFieldAccess(boolean directFieldAccess) {
		this.directFieldAccess = directFieldAccess;
	}

	/**
	 * Return whether to use direct field access instead of bean property access.
	 */
	public boolean isDirectFieldAccess() {
		return this.directFieldAccess;
	}

	/**
	 * Set whether to bind only fields intended for binding as described in
	 * {@link org.springframework.validation.DataBinder#setDeclarativeBinding}.
	 * @since 6.1
	 */
	public void setDeclarativeBinding(boolean declarativeBinding) {
		this.declarativeBinding = declarativeBinding;
	}

	/**
	 * Return whether to bind only fields intended for binding.
	 * @since 6.1
	 */
	public boolean isDeclarativeBinding() {
		return (this.declarativeBinding != null ? this.declarativeBinding : false);
	}

	/**
	 * Set the strategy to use for resolving errors into message codes.
	 * Applies the given strategy to all data binders used by this controller.
	 * <p>Default is {@code null}, i.e. using the default strategy of
	 * the data binder.
	 * @see org.springframework.validation.DataBinder#setMessageCodesResolver
	 */
	public final void setMessageCodesResolver(@Nullable MessageCodesResolver messageCodesResolver) {
		this.messageCodesResolver = messageCodesResolver;
	}

	/**
	 * Return the strategy to use for resolving errors into message codes.
	 */
	public final @Nullable MessageCodesResolver getMessageCodesResolver() {
		return this.messageCodesResolver;
	}

	/**
	 * Set the strategy to use for processing binding errors, that is,
	 * required field errors and {@code PropertyAccessException}s.
	 * <p>Default is {@code null}, that is, using the default strategy
	 * of the data binder.
	 * @see org.springframework.validation.DataBinder#setBindingErrorProcessor
	 */
	public final void setBindingErrorProcessor(@Nullable BindingErrorProcessor bindingErrorProcessor) {
		this.bindingErrorProcessor = bindingErrorProcessor;
	}

	/**
	 * Return the strategy to use for processing binding errors.
	 */
	public final @Nullable BindingErrorProcessor getBindingErrorProcessor() {
		return this.bindingErrorProcessor;
	}

	/**
	 * Set the Validator to apply after each binding step.
	 */
	public final void setValidator(@Nullable Validator validator) {
		this.validator = validator;
	}

	/**
	 * Return the Validator to apply after each binding step, if any.
	 */
	public final @Nullable Validator getValidator() {
		return this.validator;
	}

	/**
	 * Specify a ConversionService which will apply to every DataBinder.
	 * @since 3.0
	 */
	public final void setConversionService(@Nullable ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Return the ConversionService which will apply to every DataBinder.
	 */
	public final @Nullable ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Specify a single PropertyEditorRegistrar to be applied to every DataBinder.
	 */
	public final void setPropertyEditorRegistrar(PropertyEditorRegistrar propertyEditorRegistrar) {
		this.propertyEditorRegistrars = new PropertyEditorRegistrar[] {propertyEditorRegistrar};
	}

	/**
	 * Specify multiple PropertyEditorRegistrars to be applied to every DataBinder.
	 */
	public final void setPropertyEditorRegistrars(PropertyEditorRegistrar @Nullable [] propertyEditorRegistrars) {
		this.propertyEditorRegistrars = propertyEditorRegistrars;
	}

	/**
	 * Return the PropertyEditorRegistrars to be applied to every DataBinder.
	 */
	public final PropertyEditorRegistrar @Nullable [] getPropertyEditorRegistrars() {
		return this.propertyEditorRegistrars;
	}


	@Override
	public void initBinder(WebDataBinder binder) {
		binder.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
		if (this.directFieldAccess) {
			binder.initDirectFieldAccess();
		}
		if (this.declarativeBinding != null) {
			binder.setDeclarativeBinding(this.declarativeBinding);
		}
		if (this.messageCodesResolver != null) {
			binder.setMessageCodesResolver(this.messageCodesResolver);
		}
		if (this.bindingErrorProcessor != null) {
			binder.setBindingErrorProcessor(this.bindingErrorProcessor);
		}
		if (this.validator != null) {
			Class<?> type = getTargetType(binder);
			if (type != null && this.validator.supports(type)) {
				binder.setValidator(this.validator);
			}
		}
		if (this.conversionService != null) {
			binder.setConversionService(this.conversionService);
		}
		if (this.propertyEditorRegistrars != null) {
			for (PropertyEditorRegistrar propertyEditorRegistrar : this.propertyEditorRegistrars) {
				propertyEditorRegistrar.registerCustomEditors(binder);
			}
		}
	}

	private static @Nullable Class<?> getTargetType(WebDataBinder binder) {
		Class<?> type = null;
		if (binder.getTarget() != null) {
			type = binder.getTarget().getClass();
		}
		else if (binder.getTargetType() != null) {
			type = binder.getTargetType().resolve();
		}
		return type;
	}

}
