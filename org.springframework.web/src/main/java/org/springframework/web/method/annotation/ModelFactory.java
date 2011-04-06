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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.support.BindingAwareModelMap;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;

/**
 * Provides methods to create and update the "implicit" model for a given request.   
 *  
 * <p>{@link #createModel(NativeWebRequest, HandlerMethod)} prepares a model for use with 
 * a {@link RequestMapping} method. The model is populated with handler session attributes as well 
 * as request attributes obtained by invoking model attribute methods.
 * 
 * <p>{@link #updateAttributes(NativeWebRequest, SessionStatus, ModelMap, ModelMap)} updates
 * the model used for the invocation of a {@link RequestMapping} method, adding handler session 
 * attributes and {@link BindingResult} structures as necessary.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ModelFactory {

	private final List<InvocableHandlerMethod> attributeMethods;
	
	private final WebDataBinderFactory binderFactory;
	
	private final SessionAttributesHandler sessionHandler;
	
	/**
	 * Create a ModelFactory instance with the provided {@link ModelAttribute} methods.
	 * @param attributeMethods {@link ModelAttribute}-annotated methods to invoke when populating a model
	 * @param binderFactory the binder factory to use to add {@link BindingResult} instances to the model
	 * @param sessionHandler a session attributes handler to synch attributes in the model with the session
	 */
	public ModelFactory(List<InvocableHandlerMethod> attributeMethods,
						WebDataBinderFactory binderFactory,
						SessionAttributesHandler sessionHandler) {
		this.attributeMethods = attributeMethods;
		this.binderFactory = binderFactory;
		this.sessionHandler = sessionHandler;
	}

	/**
	 * Prepare a model for the current request obtaining attributes in the following order:
	 * <ol>
	 * <li>Retrieve previously accessed handler session attributes from the session
	 * <li>Invoke model attribute methods
	 * <li>Find request-handling method {@link ModelAttribute}-annotated arguments that are handler session attributes
	 * </ol>
	 * <p>As a general rule a model attribute is added only once following the above order.
	 * @param request the current request
	 * @param requestMethod the request handling method for which the model is needed
	 * @return the created model
	 * @throws Exception if an exception occurs while invoking model attribute methods
	 */
	public ModelMap createModel(NativeWebRequest request, HandlerMethod requestMethod) throws Exception {
		ExtendedModelMap model = new BindingAwareModelMap();

		Map<String, ?> sessionAttributes = this.sessionHandler.retrieveHandlerSessionAttributes(request);
		model.addAllAttributes(sessionAttributes);
		
		invokeAttributeMethods(request, model);
		
		addSessionAttributesByName(request, requestMethod, model);
		
		return model;
	}

	/**
	 * Populate the model by invoking model attribute methods. If two methods provide the same attribute, 
	 * the attribute produced by the first method is used.
	 */
	private void invokeAttributeMethods(NativeWebRequest request, ExtendedModelMap model) throws Exception {
		for (InvocableHandlerMethod attrMethod : this.attributeMethods) {
			String modelName = attrMethod.getMethodAnnotation(ModelAttribute.class).value();
			if (StringUtils.hasText(modelName) && model.containsAttribute(modelName)) {
				continue;
			}
			
			Object returnValue = attrMethod.invokeForRequest(request, model);

			if (!attrMethod.isVoid()){
				String valueName = getNameForReturnValue(returnValue, attrMethod.getReturnType());
				if (!model.containsAttribute(valueName)) {
					model.addAttribute(valueName, returnValue);
				}
			}
		}
	}
	
	/**
	 * Derive the model name for the given method return value using one of the following:
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
	 * Find request-handling method, {@link ModelAttribute}-annotated arguments that are handler session attributes 
	 * and add them to the model if not present.  
	 */
	private void addSessionAttributesByName(NativeWebRequest request, HandlerMethod requestMethod, ModelMap model) {
		for (MethodParameter parameter : requestMethod.getMethodParameters()) {
			if (!parameter.hasParameterAnnotation(ModelAttribute.class)) {
				continue;
			}
			String attrName = getNameForParameter(parameter);
			if (!model.containsKey(attrName)) {
				if (sessionHandler.isHandlerSessionAttribute(attrName, parameter.getParameterType())) {
					Object attrValue = sessionHandler.retrieveAttribute(request, attrName);
					if (attrValue == null){
						new HttpSessionRequiredException("Session attribute '" + attrName + "' not found in session");
					}
					model.addAttribute(attrName, attrValue);
				}
			}
		}
	}
	
	/**
	 * Derives the model name for the given method parameter using one of the following:
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
	 * Clean up handler session attributes when {@link SessionStatus#isComplete()} is {@code true}.
	 * Promote model attributes to the session. Add {@link BindingResult} attributes where missing.
	 * @param request the current request
	 * @param sessionStatus indicates whether handler session attributes are to be cleaned 
	 * @param actualModel the model returned from the request method, or {@code null} when the response was handled
	 * @param implicitModel the model for the request
	 * @throws Exception if the process of creating {@link BindingResult} attributes causes a problem
	 */
	public void updateAttributes(NativeWebRequest request, 
								 SessionStatus sessionStatus, 
								 ModelMap actualModel, 
								 ModelMap implicitModel) throws Exception {
		if (sessionStatus.isComplete()){
			this.sessionHandler.cleanupHandlerSessionAttributes(request);
		}
		
		if (actualModel != null) {
			this.sessionHandler.storeHandlerSessionAttributes(request, actualModel);
			updateBindingResult(request, actualModel);
		} 
		else {
			this.sessionHandler.storeHandlerSessionAttributes(request, implicitModel);
		}
	}

	/**
	 * Add {@link BindingResult} structures to the model for attributes that require it.
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
		if (this.sessionHandler.isHandlerSessionAttribute(attributeName, attrType)) {
			return true;
		}
		
		return (value != null && !value.getClass().isArray() && !(value instanceof Collection) && 
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}
	
}