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

package org.springframework.web.method.annotation;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.annotation.ValidationAnnotationUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolve {@code @ModelAttribute} annotated method arguments and handle
 * return values from {@code @ModelAttribute} annotated methods.
 *
 * <p>Model attributes are obtained from the model or created with a default
 * constructor (and then added to the model). Once created the attribute is
 * populated via data binding to Servlet request parameters. Validation may be
 * applied if the argument is annotated with {@code @jakarta.validation.Valid}.
 * or Spring's own {@code @org.springframework.validation.annotation.Validated}.
 *
 * <p>When this handler is created with {@code annotationNotRequired=true}
 * any non-simple type argument and return value is regarded as a model
 * attribute with or without the presence of an {@code @ModelAttribute}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Vladislav Kisel
 * @since 3.1
 */
public class ModelAttributeMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private final boolean annotationNotRequired;


	/**
	 * Class constructor.
	 * @param annotationNotRequired if "true", non-simple method arguments and
	 * return values are considered model attributes with or without a
	 * {@code @ModelAttribute} annotation
	 */
	public ModelAttributeMethodProcessor(boolean annotationNotRequired) {
		this.annotationNotRequired = annotationNotRequired;
	}


	/**
	 * Returns {@code true} if the parameter is annotated with
	 * {@link ModelAttribute} or, if in default resolution mode, for any
	 * method parameter that is not a simple type.
	 */
	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(parameter.getParameterType())));
	}

	/**
	 * Resolve the argument from the model or if not found instantiate it with
	 * its default if it is available. The model attribute is then populated
	 * with request values via data binding and optionally validated
	 * if {@code @java.validation.Valid} is present on the argument.
	 * @throws BindException if data binding and validation result in an error
	 * and the next method parameter is not of type {@link Errors}
	 * @throws Exception if WebDataBinder initialization fails
	 */
	@Override
	@Nullable
	public final Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

		Assert.state(mavContainer != null, "ModelAttributeMethodProcessor requires ModelAndViewContainer");
		Assert.state(binderFactory != null, "ModelAttributeMethodProcessor requires WebDataBinderFactory");

		String name = ModelFactory.getNameForParameter(parameter);
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		if (ann != null) {
			mavContainer.setBinding(name, ann.binding());
		}

		Object attribute;
		BindingResult bindingResult = null;

		if (mavContainer.containsAttribute(name)) {
			attribute = mavContainer.getModel().get(name);
			if (attribute == null || ObjectUtils.unwrapOptional(attribute) == null) {
				bindingResult = binderFactory.createBinder(webRequest, null, name).getBindingResult();
				attribute = wrapAsOptionalIfNecessary(parameter, null);
			}
			}
		else {
			try {
				// Mainly to allow subclasses alternative to create attribute
				attribute = createAttribute(name, parameter, binderFactory, webRequest);
			}
			catch (MethodArgumentNotValidException ex) {
				if (isBindExceptionRequired(parameter)) {
					throw ex;
				}
				attribute = wrapAsOptionalIfNecessary(parameter, ex.getTarget());
				bindingResult = ex.getBindingResult();
			}
		}

		// No BindingResult yet, proceed with binding and validation
		if (bindingResult == null) {
			ResolvableType type = ResolvableType.forMethodParameter(parameter);
			WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name, type);
			if (attribute == null) {
				constructAttribute(binder, webRequest);
				attribute = wrapAsOptionalIfNecessary(parameter, binder.getTarget());
			}
			if (!binder.getBindingResult().hasErrors()) {
				if (!mavContainer.isBindingDisabled(name)) {
					bindRequestParameters(binder, webRequest);
				}
				validateIfApplicable(binder, parameter);
			}
			if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
				throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
			}
			// Value type adaptation, also covering java.util.Optional
			if (!parameter.getParameterType().isInstance(attribute)) {
				attribute = binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter);
			}
			bindingResult = binder.getBindingResult();
		}

		// Add resolved attribute and BindingResult at the end of the model
		Map<String, Object> bindingResultModel = bindingResult.getModel();
		mavContainer.removeAttributes(bindingResultModel);
		mavContainer.addAllAttributes(bindingResultModel);

		return attribute;
	}

	@Nullable
	private static Object wrapAsOptionalIfNecessary(MethodParameter parameter, @Nullable Object target) {
		return (parameter.getParameterType() == Optional.class ? Optional.ofNullable(target) : target);
	}

	/**
	 * Extension point to create the model attribute if not found in the model,
	 * with subsequent parameter binding through bean properties (unless suppressed).
	 * <p>By default, as of 6.1 this method returns {@code null} in which case
	 * {@link org.springframework.validation.DataBinder#construct} is used instead
	 * to create the model attribute. The main purpose of this method then is to
	 * allow to create the model attribute in some other, alternative way.
	 * @param attributeName the name of the attribute (never {@code null})
	 * @param parameter the method parameter declaration
	 * @param binderFactory for creating WebDataBinder instance
	 * @param request the current request
	 * @return the created model attribute, or {@code null}
	 */
	@Nullable
	protected Object createAttribute(String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest request) throws Exception {

		return null;
	}

	/**
	 * Extension point to create the attribute, binding the request to constructor args.
	 * @param binder the data binder instance to use for the binding
	 * @param request the current request
	 * @since 6.1
	 */
	protected void constructAttribute(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).construct(request);
	}

	/**
	 * Extension point to bind the request to the target object via setters/fields.
	 * @param binder the data binder instance to use for the binding
	 * @param request the current request
	 */
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).bind(request);
	}

	/**
	 * Validate the model attribute if applicable.
	 * <p>The default implementation checks for {@code @jakarta.validation.Valid},
	 * Spring's {@link org.springframework.validation.annotation.Validated},
	 * and custom annotations whose name starts with "Valid".
	 * @param binder the DataBinder to be used
	 * @param parameter the method parameter declaration
	 * @see WebDataBinder#validate(Object...)
	 * @see SmartValidator#validate(Object, Errors, Object...)
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		for (Annotation ann : parameter.getParameterAnnotations()) {
			Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);
			if (validationHints != null) {
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * Whether to raise a fatal bind exception on validation errors.
	 * <p>The default implementation delegates to {@link #isBindExceptionRequired(MethodParameter)}.
	 * @param binder the data binder used to perform data binding
	 * @param parameter the method parameter declaration
	 * @return {@code true} if the next method parameter is not of type {@link Errors}
	 * @see #isBindExceptionRequired(MethodParameter)
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		return isBindExceptionRequired(parameter);
	}

	/**
	 * Whether to raise a fatal bind exception on validation errors.
	 * @param parameter the method parameter declaration
	 * @return {@code true} if the next method parameter is not of type {@link Errors}
	 * @since 5.0
	 */
	protected boolean isBindExceptionRequired(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getExecutable().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		return !hasBindingResult;
	}

	/**
	 * Return {@code true} if there is a method-level {@code @ModelAttribute}
	 * or, in default resolution mode, for any return value type that is not
	 * a simple type.
	 */
	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return (returnType.hasMethodAnnotation(ModelAttribute.class) ||
				(this.annotationNotRequired && !BeanUtils.isSimpleProperty(returnType.getParameterType())));
	}

	/**
	 * Add non-null return values to the {@link ModelAndViewContainer}.
	 */
	@Override
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue != null) {
			String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
			mavContainer.addAttribute(name, returnValue);
		}
	}

}
