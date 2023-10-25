/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.Annotation;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.validation.DataBinder;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Create a {@link WebRequestDataBinder} instance and initialize it with a
 * {@link WebBindingInitializer}.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultDataBinderFactory implements WebDataBinderFactory {

	@Nullable
	private final WebBindingInitializer initializer;

	private boolean methodValidationApplicable;


	/**
	 * Create a new {@code DefaultDataBinderFactory} instance.
	 * @param initializer for global data binder initialization
	 * (or {@code null} if none)
	 */
	public DefaultDataBinderFactory(@Nullable WebBindingInitializer initializer) {
		this.initializer = initializer;
	}


	/**
	 * Configure flag to signal whether validation will be applied to handler
	 * method arguments, which is the case if Bean Validation is enabled in
	 * Spring MVC, and method parameters have {@code @Constraint} annotations.
	 * @since 6.1
	 */
	public void setMethodValidationApplicable(boolean methodValidationApplicable) {
		this.methodValidationApplicable = methodValidationApplicable;
	}


	/**
	 * Create a new {@link WebDataBinder} for the given target object and
	 * initialize it through a {@link WebBindingInitializer}.
	 * @throws Exception in case of invalid state or arguments
	 */
	@Override
	public final WebDataBinder createBinder(
			NativeWebRequest webRequest, @Nullable Object target, String objectName) throws Exception {

		return createBinderInternal(webRequest, target, objectName, null);
	}

	/**
	 * {@inheritDoc}.
	 * <p>By default, if the parameter has {@code @Valid}, Bean Validation is
	 * excluded, deferring to method validation.
	 */
	@Override
	public final WebDataBinder createBinder(
			NativeWebRequest webRequest, @Nullable Object target, String objectName,
			ResolvableType type) throws Exception {

		return createBinderInternal(webRequest, target, objectName, type);
	}

	private WebDataBinder createBinderInternal(
			NativeWebRequest webRequest, @Nullable Object target, String objectName,
			@Nullable ResolvableType type) throws Exception {

		WebDataBinder dataBinder = createBinderInstance(target, objectName, webRequest);
		dataBinder.setNameResolver(new BindParamNameResolver());

		if (target == null && type != null) {
			dataBinder.setTargetType(type);
		}

		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}

		initBinder(dataBinder, webRequest);

		if (this.methodValidationApplicable && type != null) {
			if (type.getSource() instanceof MethodParameter parameter) {
				MethodValidationInitializer.initBinder(dataBinder, parameter);
			}
		}

		return dataBinder;
	}

	/**
	 * Extension point to create the WebDataBinder instance.
	 * By default, this is {@code WebRequestDataBinder}.
	 * @param target the binding target or {@code null} for type conversion only
	 * @param objectName the binding target object name
	 * @param webRequest the current request
	 * @throws Exception in case of invalid state or arguments
	 */
	protected WebDataBinder createBinderInstance(
			@Nullable Object target, String objectName, NativeWebRequest webRequest) throws Exception {

		return new WebRequestDataBinder(target, objectName);
	}

	/**
	 * Extension point to further initialize the created data binder instance
	 * (e.g. with {@code @InitBinder} methods) after "global" initialization
	 * via {@link WebBindingInitializer}.
	 * @param dataBinder the data binder instance to customize
	 * @param webRequest the current request
	 * @throws Exception if initialization fails
	 */
	protected void initBinder(WebDataBinder dataBinder, NativeWebRequest webRequest)
			throws Exception {

	}


	/**
	 * Excludes Bean Validation if the method parameter has {@code @Valid}.
	 */
	private static class MethodValidationInitializer {

		public static void initBinder(DataBinder binder, MethodParameter parameter) {
			for (Annotation annotation : parameter.getParameterAnnotations()) {
				if (annotation.annotationType().getName().equals("jakarta.validation.Valid")) {
					binder.setExcludedValidators(validator -> validator instanceof jakarta.validation.Validator);
				}
			}
		}
	}

}
