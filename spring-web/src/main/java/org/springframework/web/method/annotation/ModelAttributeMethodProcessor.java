/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.beans.ConstructorProperties;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
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
 * applied if the argument is annotated with {@code @javax.validation.Valid}.
 * or Spring's own {@code @org.springframework.validation.annotation.Validated}.
 *
 * <p>When this handler is created with {@code annotationNotRequired=true}
 * any non-simple type argument and return value is regarded as a model
 * attribute with or without the presence of an {@code @ModelAttribute}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ModelAttributeMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	private static final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

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
	public final Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		String name = ModelFactory.getNameForParameter(parameter);
		Object attribute = (mavContainer.containsAttribute(name) ? mavContainer.getModel().get(name) :
				createAttribute(name, parameter, binderFactory, webRequest));

		if (!mavContainer.isBindingDisabled(name)) {
			ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
			if (ann != null && !ann.binding()) {
				mavContainer.setBindingDisabled(name);
			}
		}

		WebDataBinder binder = binderFactory.createBinder(webRequest, attribute, name);
		if (binder.getTarget() != null) {
			if (!mavContainer.isBindingDisabled(name)) {
				bindRequestParameters(binder, webRequest);
			}
			validateIfApplicable(binder, parameter);
			if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
				throw new BindException(binder.getBindingResult());
			}
		}

		// Add resolved attribute and BindingResult at the end of the model
		Map<String, Object> bindingResultModel = binder.getBindingResult().getModel();
		mavContainer.removeAttributes(bindingResultModel);
		mavContainer.addAllAttributes(bindingResultModel);

		return (parameter.getParameterType().isInstance(attribute) ? attribute :
				binder.convertIfNecessary(binder.getTarget(), parameter.getParameterType(), parameter));
	}

	/**
	 * Extension point to create the model attribute if not found in the model,
	 * with subsequent parameter binding through bean properties (unless suppressed).
	 * <p>The default implementation uses the unique public no-arg constructor, if any,
	 * which may have arguments: It understands the JavaBeans {@link ConstructorProperties}
	 * annotation as well as runtime-retained parameter names in the bytecode,
	 * associating request parameters with constructor arguments by name. If no such
	 * constructor is found, the default constructor will be used (even if not public),
	 * assuming subsequent bean property bindings through setter methods.
	 * @param attributeName the name of the attribute (never {@code null})
	 * @param parameter the method parameter declaration
	 * @param binderFactory for creating WebDataBinder instance
	 * @param webRequest the current request
	 * @return the created model attribute (never {@code null})
	 */
	protected Object createAttribute(String attributeName, MethodParameter parameter,
			WebDataBinderFactory binderFactory, NativeWebRequest webRequest) throws Exception {

		Constructor<?>[] ctors = parameter.getParameterType().getConstructors();
		if (ctors.length != 1) {
			// No standard data class or standard JavaBeans arrangement ->
			// defensively go with default constructor, expecting regular bean property bindings.
			return BeanUtils.instantiateClass(parameter.getParameterType());
		}
		Constructor<?> ctor = ctors[0];
		if (ctor.getParameterCount() == 0) {
			// A single default constructor -> clearly a standard JavaBeans arrangement.
			return BeanUtils.instantiateClass(ctor);
		}

		// A single data class constructor -> resolve constructor arguments from request parameters.
		ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
		String[] paramNames = (cp != null ? cp.value() : parameterNameDiscoverer.getParameterNames(ctor));
		Assert.state(paramNames != null, () -> "Cannot resolve parameter names for constructor " + ctor);
		Class<?>[] paramTypes = ctor.getParameterTypes();
		Assert.state(paramNames.length == paramTypes.length,
				() -> "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor);
		Object[] args = new Object[paramTypes.length];
		WebDataBinder binder = binderFactory.createBinder(webRequest, null, attributeName);
		for (int i = 0; i < paramNames.length; i++) {
			args[i] = binder.convertIfNecessary(
					webRequest.getParameterValues(paramNames[i]), paramTypes[i], new MethodParameter(ctor, i));
		}
		return BeanUtils.instantiateClass(ctor, args);
	}

	/**
	 * Extension point to bind the request to the target object.
	 * @param binder the data binder instance to use for the binding
	 * @param request the current request
	 */
	protected void bindRequestParameters(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).bind(request);
	}

	/**
	 * Validate the model attribute if applicable.
	 * <p>The default implementation checks for {@code @javax.validation.Valid},
	 * Spring's {@link org.springframework.validation.annotation.Validated},
	 * and custom annotations whose name starts with "Valid".
	 * @param binder the DataBinder to be used
	 * @param parameter the method parameter declaration
	 */
	protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation ann : annotations) {
			Validated validatedAnn = AnnotationUtils.getAnnotation(ann, Validated.class);
			if (validatedAnn != null || ann.annotationType().getSimpleName().startsWith("Valid")) {
				Object hints = (validatedAnn != null ? validatedAnn.value() : AnnotationUtils.getValue(ann));
				Object[] validationHints = (hints instanceof Object[] ? (Object[]) hints : new Object[] {hints});
				binder.validate(validationHints);
				break;
			}
		}
	}

	/**
	 * Whether to raise a fatal bind exception on validation errors.
	 * @param binder the data binder used to perform data binding
	 * @param parameter the method parameter declaration
	 * @return {@code true} if the next method argument is not of type {@link Errors}
	 */
	protected boolean isBindExceptionRequired(WebDataBinder binder, MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
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
	public void handleReturnValue(Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		if (returnValue != null) {
			String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
			mavContainer.addAttribute(name, returnValue);
		}
	}

}
