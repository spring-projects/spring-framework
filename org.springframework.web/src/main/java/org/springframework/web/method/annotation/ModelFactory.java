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
 * Contains methods for creating and updating a model. A {@link ModelFactory} is associated with a specific controller
 * through knowledge of its @{@link ModelAttribute} methods and @{@link SessionAttributes}. 
 * 
 * <p>{@link #initModel(NativeWebRequest, ModelAndViewContainer, HandlerMethod)} populates the model
 * with handler session attributes and by invoking model attribute methods.
 * 
 * <p>{@link #updateModel(NativeWebRequest, ModelAndViewContainer, SessionStatus)} updates
 * the model (usually after the {@link RequestMapping} method has been called) promoting attributes
 * to the session and adding {@link BindingResult} attributes as necessary.
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
	 * @param binderFactory the binder factory to use when adding {@link BindingResult}s to the model
	 * @param sessionHandler a session attributes handler to synch attributes with the session
	 */
	public ModelFactory(List<InvocableHandlerMethod> attributeMethods,
						WebDataBinderFactory binderFactory,
						SessionAttributesHandler sessionHandler) {
		this.attributeMethods = (attributeMethods != null) ? attributeMethods : new ArrayList<InvocableHandlerMethod>();
		this.binderFactory = binderFactory;
		this.sessionHandler = sessionHandler;
	}

	/**
	 * Populate the model for a request with attributes obtained in the following order:
	 * <ol>
	 * <li>Retrieve "known" (i.e. have been in the model in prior requests) handler session attributes from the session 
	 * <li>Create attributes by invoking model attribute methods
	 * <li>Check for not yet known handler session attributes in the session  
	 * </ol>
	 * <p>As a general rule model attributes are added only once following the above order.
	 * 
	 * @param request the current request
	 * @param mavContainer the {@link ModelAndViewContainer} to add model attributes to
	 * @param requestMethod the request handling method for which the model is needed
	 * @throws Exception if an exception occurs while invoking model attribute methods
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer mavContainer, HandlerMethod requestMethod) 
			throws Exception {
		
		Map<String, ?> sessionAttributes = this.sessionHandler.retrieveHandlerSessionAttributes(request);
		mavContainer.addAllAttributes(sessionAttributes);
		
		invokeAttributeMethods(request, mavContainer);
		
		checkMissingSessionAttributes(request, mavContainer, requestMethod);
	}

	/**
	 * Invoke model attribute methods to populate the model. 
	 * If two methods return the same attribute, the attribute from the first method is added.
	 */
	private void invokeAttributeMethods(NativeWebRequest request, ModelAndViewContainer mavContainer)
			throws Exception {
		
		for (InvocableHandlerMethod attrMethod : this.attributeMethods) {
			String modelName = attrMethod.getMethodAnnotation(ModelAttribute.class).value();
			if (mavContainer.containsAttribute(modelName)) {
				continue;
			}
			
			Object returnValue = attrMethod.invokeForRequest(request, mavContainer);

			if (!attrMethod.isVoid()){
				String valueName = getNameForReturnValue(returnValue, attrMethod.getReturnType());
				mavContainer.mergeAttribute(valueName, returnValue);
			}
		}
	}
	
	/**
	 * Checks if any {@link ModelAttribute}-annotated handler method arguments are eligible as handler session 
	 * attributes, as defined by @{@link SessionAttributes}, and are not yet present in the model. 
	 * If so, attempts to retrieve them from the session and add them to the model.
	 * 
	 * @throws HttpSessionRequiredException raised if a handler session attribute could is missing 
	 */
	private void checkMissingSessionAttributes(NativeWebRequest request, 
											   ModelAndViewContainer mavContainer, 
											   HandlerMethod requestMethod) throws HttpSessionRequiredException {
		for (MethodParameter parameter : requestMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				String name = getNameForParameter(parameter);
				if (!mavContainer.containsAttribute(name)) {
					if (sessionHandler.isHandlerSessionAttribute(name, parameter.getParameterType())) {
						Object attrValue = sessionHandler.retrieveAttribute(request, name);
						if (attrValue == null){
							throw new HttpSessionRequiredException(
									"Session attribute '" + name + "' not found in session: " + requestMethod);
						}
						mavContainer.addAttribute(name, attrValue);
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
	 * @param sessionStatus whether session processing is complete 
	 * @throws Exception if the process of creating {@link BindingResult} attributes causes an error
	 */
	public void updateModel(NativeWebRequest request, ModelAndViewContainer mavContainer, SessionStatus sessionStatus) 
			throws Exception {
		
		if (sessionStatus.isComplete()){
			this.sessionHandler.cleanupHandlerSessionAttributes(request);
		}

		this.sessionHandler.storeHandlerSessionAttributes(request, mavContainer.getModel());

		if (mavContainer.isResolveView()) {
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
		if (this.sessionHandler.isHandlerSessionAttribute(attributeName, attrType)) {
			return true;
		}
		
		return (value != null && !value.getClass().isArray() && !(value instanceof Collection) && 
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}
	
}