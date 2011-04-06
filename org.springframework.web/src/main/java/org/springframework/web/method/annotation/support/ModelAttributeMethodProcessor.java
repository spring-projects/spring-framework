/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.method.annotation.support;

import java.lang.annotation.Annotation;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.SessionAttributeStore;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves model attribute method parameters. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAttributeMethodProcessor
		implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	private final boolean resolveArgumentsWithoutAnnotations;
	
	/**
	 * Creates a {@link ModelAttributeMethodProcessor} instance.
	 * @param resolveArgumentsWithoutAnnotations enable default resolution mode in which arguments without
	 * 		annotations that aren't simple types (see {@link BeanUtils#isSimpleProperty(Class)})  
	 * 		are also treated as model attributes with a default name based on the model attribute type.
	 */
	public ModelAttributeMethodProcessor(boolean resolveArgumentsWithoutAnnotations) {
		this.resolveArgumentsWithoutAnnotations = resolveArgumentsWithoutAnnotations;
	}

	/**
	 * @return true if the parameter is annotated with {@link ModelAttribute} or if it is a 
	 * 		simple type without any annotations. 
	 */
	public boolean supportsParameter(MethodParameter parameter) {
		if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
			return true;
		}
		else if (this.resolveArgumentsWithoutAnnotations && !parameter.hasParameterAnnotations()) {
			return !BeanUtils.isSimpleProperty(parameter.getParameterType());
		}
		else {
			return false;
		}
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	/**
	 * Creates a {@link WebDataBinder} for the target model attribute and applies data binding to it.
	 * The model attribute may be obtained from the "implicit" model, from the session via, or by 
	 * direct instantiation.
	 * 
	 * @throws Exception if invoking an data binder initialization fails or if data binding and/or 
	 * 		validation results in errors and the next method parameter is not of type {@link Errors}.
	 */
	public final Object resolveArgument(MethodParameter parameter, 
										ModelMap model, 
										NativeWebRequest webRequest,
										WebDataBinderFactory binderFactory) throws Exception {
		WebDataBinder binder = createDataBinder(parameter, model, webRequest, binderFactory);

		if (binder.getTarget() != null) {
			doBind(binder, webRequest);

			if (shouldValidate(parameter)) {
				binder.validate();
			}

			if (failOnError(parameter) && binder.getBindingResult().hasErrors()) {
				throw new BindException(binder.getBindingResult());
			}
		}

		model.putAll(binder.getBindingResult().getModel());

		return binder.getTarget();
	}

	/**
	 * Creates a {@link WebDataBinder} for a target object which may be obtained from the "implicit" model,
	 * the session via {@link SessionAttributeStore}, or by direct instantiation.  
	 */
	private WebDataBinder createDataBinder(MethodParameter parameter, 
										   ModelMap model, 
										   NativeWebRequest webRequest, 
										   WebDataBinderFactory binderFactory) throws Exception {
		String attrName = ModelFactory.getNameForParameter(parameter);
		
		Object target;
		if (model.containsKey(attrName)) {
			target = model.get(attrName);
		}
		else {
			target = BeanUtils.instantiateClass(parameter.getParameterType());
		}
		
		return binderFactory.createBinder(webRequest, target, attrName);
	}
	
	protected void doBind(WebDataBinder binder, NativeWebRequest request) {
		((WebRequestDataBinder) binder).bind(request);
	}

	/**
	 * @return true if {@link DataBinder#validate()} should be invoked, false otherwise.
	 */
	protected boolean shouldValidate(MethodParameter parameter) {
		Annotation[] annotations = parameter.getParameterAnnotations();
		for (Annotation annot : annotations) {
			if ("Valid".equals(annot.annotationType().getSimpleName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @return true if the binding or validation errors should result in a {@link BindException}, false otherwise.
	 */
	protected boolean failOnError(MethodParameter parameter) {
		int i = parameter.getParameterIndex();
		Class<?>[] paramTypes = parameter.getMethod().getParameterTypes();
		boolean hasBindingResult = (paramTypes.length > (i + 1) && Errors.class.isAssignableFrom(paramTypes[i + 1]));
		
		return !hasBindingResult;
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		return returnType.getMethodAnnotation(ModelAttribute.class) != null;
	}

	public <V> void handleReturnValue(Object returnValue, 
									  MethodParameter returnType, 
									  ModelAndViewContainer<V> mavContainer,
									  NativeWebRequest webRequest) throws Exception {
		String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
		mavContainer.addModelAttribute(name, returnValue);
	}

}