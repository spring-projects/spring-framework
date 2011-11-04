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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Provides methods to initialize the {@link Model} before controller method 
 * invocation and to update it afterwards. On initialization, the model is 
 * populated with attributes from the session or by invoking 
 * {@code @ModelAttribute} methods. On update, model attributes are 
 * synchronized with the session -- either adding or removing them. 
 * Also {@link BindingResult} attributes where missing.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ModelFactory {

	private final List<InvocableHandlerMethod> attributeMethods;
	
	private final WebDataBinderFactory binderFactory;
	
	private final SessionAttributesHandler sessionAttributesHandler;
	
	/**
	 * Create a new instance with the given {@code @ModelAttribute} methods.
	 * @param attributeMethods for model initialization
	 * @param binderFactory for adding {@link BindingResult} attributes
	 * @param sessionAttributesHandler for access to session attributes
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
	 * 	<li>Retrieve "known" session attributes -- i.e. attributes listed via 
	 * 	{@link SessionAttributes @SessionAttributes} and previously stored in 
	 * 	the in the model at least once
	 * 	<li>Invoke {@link ModelAttribute @ModelAttribute} methods
	 * 	<li>Find method arguments eligible as session attributes and retrieve
	 * 	them if they're not	already	present in the model 
	 * </ol>
	 * @param request the current request
	 * @param mavContainer contains the model to be initialized 
	 * @param handlerMethod the method for which the model is initialized
	 * @throws Exception may arise from {@code @ModelAttribute} methods
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer mavContainer, HandlerMethod handlerMethod)
			throws Exception {

		Map<String, ?> attributesInSession = this.sessionAttributesHandler.retrieveAttributes(request);
		mavContainer.mergeAttributes(attributesInSession);

		invokeModelAttributeMethods(request, mavContainer);

		for (String name : findSessionAttributeArguments(handlerMethod)) {
			if (!mavContainer.containsAttribute(name)) {
				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
				if (value == null) {
					throw new HttpSessionRequiredException("Expected session attribute '" + name + "'");
				}
				mavContainer.addAttribute(name, value);
			}
		}
	}

	/**
	 * Invoke model attribute methods to populate the model. Attributes are 
	 * added only if not already present in the model.
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
	 * Return all {@code @ModelAttribute} arguments declared as session 
	 * attributes via {@code @SessionAttributes}.
	 */
	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
		List<String> result = new ArrayList<String>();
		for (MethodParameter param : handlerMethod.getMethodParameters()) {
			if (param.hasParameterAnnotation(ModelAttribute.class)) {
				String name = getNameForParameter(param);
				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, param.getParameterType())) {
					result.add(name);
				}
			}
		}
		return result;
	}
	
	/**
	 * Derive the model attribute name for the given return value using 
	 * one of the following:
	 * <ol>
	 * 	<li>The method {@code ModelAttribute} annotation value 
	 * 	<li>The declared return type if it is other than {@code Object}
	 * 	<li>The actual return value type
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
	 * Derives the model attribute name for a method parameter based on:
	 * <ol>
	 * 	<li>The parameter {@code @ModelAttribute} annotation value
	 * 	<li>The parameter type 
	 * </ol>
	 * @return the derived name; never {@code null} or an empty string
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		ModelAttribute annot = parameter.getParameterAnnotation(ModelAttribute.class);
		String attrName = (annot != null) ? annot.value() : null;
		return StringUtils.hasText(attrName) ? attrName :  Conventions.getVariableNameForParameter(parameter);
	}
	
	/**
	 * Synchronize model attributes with the session. Add {@link BindingResult}
	 * attributes where necessary.
	 * @param request the current request
	 * @param mavContainer contains the model to update
	 * @throws Exception if creating BindingResult attributes fails
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
	 * Whether the given attribute requires a {@link BindingResult} in the model.
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