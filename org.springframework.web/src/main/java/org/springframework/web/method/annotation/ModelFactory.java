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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.core.Conventions;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Provides methods to initialize the {@link Model} before a controller method 
 * invocation and to update it after the controller method has been invoked.
 * 
 * <p>On initialization the model may be populated with session attributes 
 * stored during a previous request as a result of a {@link SessionAttributes} 
 * annotation. @{@link ModelAttribute} methods in the same controller may 
 * also be invoked to populate the model.
 * 
 * <p>On update attributes may be removed from or stored in the session.
 * {@link BindingResult} attributes may also be added as necessary.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ModelFactory {

	private final List<InvocableHandlerMethod> attributeMethods;
	
	private final WebDataBinderFactory binderFactory;
	
	private final SessionAttributesHandler sessionAttributesHandler;
	
	/**
	 * Create a ModelFactory instance with the provided {@link ModelAttribute} methods.
	 * @param attributeMethods {@link ModelAttribute} methods to initialize model instances with
	 * @param binderFactory used to add {@link BindingResult} attributes to the model
	 * @param sessionAttributesHandler used to access handler-specific session attributes
	 */
	public ModelFactory(List<InvocableHandlerMethod> attributeMethods,
						WebDataBinderFactory binderFactory,
						SessionAttributesHandler sessionAttributesHandler) {
		this.attributeMethods = (attributeMethods != null) ? attributeMethods : new ArrayList<InvocableHandlerMethod>();
		this.binderFactory = binderFactory;
		this.sessionAttributesHandler = sessionAttributesHandler;
	}

	/**
	 * Populate the model in the following order:
	 * <ol>
	 * 	<li>Retrieve "remembered" (i.e. previously stored) controller-specific session attributes
	 * 	<li>Invoke @{@link ModelAttribute} methods
	 * 	<li>Check the session for any controller-specific attributes not yet "remembered".
	 * </ol>
	 * @param request the current request
	 * @param mavContainer contains the model to initialize 
	 * @param handlerMethod the @{@link RequestMapping} method for which the model is initialized
	 * @throws Exception may arise from the invocation of @{@link ModelAttribute} methods
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer mavContainer, HandlerMethod handlerMethod)
			throws Exception {

		Map<String, ?> sessionAttrs = this.sessionAttributesHandler.retrieveAttributes(request);
		mavContainer.mergeAttributes(sessionAttrs);
		
		invokeModelAttributeMethods(request, mavContainer);
		
		checkHandlerSessionAttributes(request, mavContainer, handlerMethod);
	}

	/**
	 * Invoke model attribute methods to populate the model. 
	 * If two methods return the same attribute, the attribute from the first method is added.
	 */
	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer mavContainer)
			throws Exception {
		
		for (InvocableHandlerMethod attrMethod : this.attributeMethods) {
			String modelName = attrMethod.getMethodAnnotation(ModelAttribute.class).value();
			if (mavContainer.containsAttribute(modelName)) {
				continue;
			}
			
			Object returnValue = attrMethod.invokeForRequest(request, mavContainer);

			if (!attrMethod.isVoid()){
				String returnValueName = getNameForReturnValue(returnValue, attrMethod.getReturnType());
				if (!mavContainer.containsAttribute(returnValueName)) {
					mavContainer.addAttribute(returnValueName, returnValue);
				}
			}
		}
	}
	
	/**
	 * Checks for @{@link ModelAttribute} arguments in the signature of the 
	 * {@link RequestMapping} method that are declared as session attributes 
	 * via @{@link SessionAttributes} but are not already in the model. 
	 * Those attributes may have been outside of this controller. 
	 * Try to locate the attributes in the session or raise an exception. 
	 * 
	 * @throws HttpSessionRequiredException raised if an attribute declared
	 * as session attribute is missing.
	 */
	private void checkHandlerSessionAttributes(NativeWebRequest request, 
											   ModelAndViewContainer mavContainer, 
											   HandlerMethod handlerMethod) throws HttpSessionRequiredException {
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				String attrName = getNameForParameter(parameter);
				if (!mavContainer.containsAttribute(attrName)) {
					if (sessionAttributesHandler.isHandlerSessionAttribute(attrName, parameter.getParameterType())) {
						Object attrValue = sessionAttributesHandler.retrieveAttribute(request, attrName);
						if (attrValue == null){
							throw new HttpSessionRequiredException(
									"Session attribute '" + attrName + "' not found in session: " + handlerMethod);
						}
						mavContainer.addAttribute(attrName, attrValue);
					}
				}
			}
		}
	}
	
	/**
	 * Derive the model attribute name for the given return value using one of the following:
	 * <ol>
	 * <li>The method {@link ModelAttribute} annotation value 
	 * <li>The name of the return type
	 * <li>The name of the return value type if the method return type is {@code Object} 
	 * </ol>
	 * @param returnValue the value returned from a method invocation
	 * @param returnType the return type of the method
	 * @return the model name, never {@code null} nor empty
	 */
	public static String getNameForReturnValue(Object returnValue, MethodParameter returnType) {
		ModelAttribute annot = returnType.getMethodAnnotation(ModelAttribute.class);
		if (annot != null && StringUtils.hasText(annot.value())) {
			return annot.value();
		}
		else {
			Method method = returnType.getMethod();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, returnType.getDeclaringClass());
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}

	/**
	 * Derives the model attribute name for the given method parameter using one of the following:
	 * <ol>
	 * <li>The parameter {@link ModelAttribute} annotation value
	 * <li>The name of the parameter type 
	 * </ol>
	 * @return the method parameter model name, never {@code null} or an empty string
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
		String attrName = (annot != null) ? annot.value() : null;
		return StringUtils.hasText(attrName) ? attrName :  Conventions.getVariableNameForParameter(parameter);
	}
	
	/**
	 * Updates the model by cleaning handler session attributes depending on {@link SessionStatus#isComplete()},
	 * promotes model attributes to the session, and adds {@link BindingResult} attributes where missing.
	 * @param request the current request
	 * @param mavContainer the {@link ModelAndViewContainer} for the current request
	 * @throws Exception if the process of creating {@link BindingResult} attributes causes an error
	 */
	public void updateModel(NativeWebRequest request, ModelAndViewContainer mavContainer) throws Exception {
		
		if (mavContainer.getSessionStatus().isComplete()){
			this.sessionAttributesHandler.cleanupAttributes(request);
		}
		else {
			this.sessionAttributesHandler.storeAttributes(request, mavContainer.getModel());
		}
		
		if (!mavContainer.isRequestHandled()) {
			updateBindingResult(request, mavContainer.getModel());
		} 
	}

	/**
	 * Add {@link BindingResult} attributes to the model for attributes that require it.
	 */
	private void updateBindingResult(NativeWebRequest request, ModelMap model) throws Exception {
		List<String> keyNames = new ArrayList<String>(model.keySet());
		for (String name : keyNames) {
			Object value = model.get(name);

			if (isBindingCandidate(name, value)) {
				String bindingResultKey = BindingResult.MODEL_KEY_PREFIX + name;
			
				if (!model.containsAttribute(bindingResultKey)) {
					WebDataBinder dataBinder = binderFactory.createBinder(request, value, name);
					model.put(bindingResultKey, dataBinder.getBindingResult());
				}
			}
		}
	}

	/**
	 * Whether the given attribute requires a {@link BindingResult} added to the model.
	 */
	private boolean isBindingCandidate(String attributeName, Object value) {
		if (attributeName.startsWith(BindingResult.MODEL_KEY_PREFIX)) {
			return false;
		}
		
		Class<?> attrType = (value != null) ? value.getClass() : null;
		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, attrType)) {
			return true;
		}
		
		return (value != null && !value.getClass().isArray() && !(value instanceof Collection) && 
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}
	
}