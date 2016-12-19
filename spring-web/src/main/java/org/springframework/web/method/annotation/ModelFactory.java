/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Assist with initialization of the {@link Model} before controller method
 * invocation and with updates to it after the invocation.
 *
 * <p>On initialization the model is populated with attributes temporarily stored
 * in the session and through the invocation of {@code @ModelAttribute} methods.
 *
 * <p>On update model attributes are synchronized with the session and also
 * {@link BindingResult} attributes are added if missing.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public final class ModelFactory {

	private static final Log logger = LogFactory.getLog(ModelFactory.class);

	private final List<ModelMethod> modelMethods = new ArrayList<ModelMethod>();

	private final WebDataBinderFactory dataBinderFactory;

	private final SessionAttributesHandler sessionAttributesHandler;


	/**
	 * Create a new instance with the given {@code @ModelAttribute} methods.
	 * @param handlerMethods the {@code @ModelAttribute} methods to invoke
	 * @param binderFactory for preparation of {@link BindingResult} attributes
	 * @param attributeHandler for access to session attributes
	 */
	public ModelFactory(List<InvocableHandlerMethod> handlerMethods,
			WebDataBinderFactory binderFactory, SessionAttributesHandler attributeHandler) {

		if (handlerMethods != null) {
			for (InvocableHandlerMethod handlerMethod : handlerMethods) {
				this.modelMethods.add(new ModelMethod(handlerMethod));
			}
		}
		this.dataBinderFactory = binderFactory;
		this.sessionAttributesHandler = attributeHandler;
	}


	/**
	 * Populate the model in the following order:
	 * <ol>
	 * <li>Retrieve "known" session attributes listed as {@code @SessionAttributes}.
	 * <li>Invoke {@code @ModelAttribute} methods
	 * <li>Find {@code @ModelAttribute} method arguments also listed as
	 * {@code @SessionAttributes} and ensure they're present in the model raising
	 * an exception if necessary.
	 * </ol>
	 * @param request the current request
	 * @param container a container with the model to be initialized
	 * @param handlerMethod the method for which the model is initialized
	 * @throws Exception may arise from {@code @ModelAttribute} methods
	 */
	public void initModel(NativeWebRequest request, ModelAndViewContainer container,
			HandlerMethod handlerMethod) throws Exception {

		Map<String, ?> sessionAttributes = this.sessionAttributesHandler.retrieveAttributes(request);
		container.mergeAttributes(sessionAttributes);
		invokeModelAttributeMethods(request, container);

		for (String name : findSessionAttributeArguments(handlerMethod)) {
			if (!container.containsAttribute(name)) {
				Object value = this.sessionAttributesHandler.retrieveAttribute(request, name);
				if (value == null) {
					throw new HttpSessionRequiredException("Expected session attribute '" + name + "'", name);
				}
				container.addAttribute(name, value);
			}
		}
	}

	/**
	 * Invoke model attribute methods to populate the model.
	 * Attributes are added only if not already present in the model.
	 */
	private void invokeModelAttributeMethods(NativeWebRequest request, ModelAndViewContainer container)
			throws Exception {

		while (!this.modelMethods.isEmpty()) {
			InvocableHandlerMethod modelMethod = getNextModelMethod(container).getHandlerMethod();
			ModelAttribute ann = modelMethod.getMethodAnnotation(ModelAttribute.class);
			if (container.containsAttribute(ann.name())) {
				if (!ann.binding()) {
					container.setBindingDisabled(ann.name());
				}
				continue;
			}

			Object returnValue = modelMethod.invokeForRequest(request, container);
			if (!modelMethod.isVoid()){
				String returnValueName = getNameForReturnValue(returnValue, modelMethod.getReturnType());
				if (!ann.binding()) {
					container.setBindingDisabled(returnValueName);
				}
				if (!container.containsAttribute(returnValueName)) {
					container.addAttribute(returnValueName, returnValue);
				}
			}
		}
	}

	private ModelMethod getNextModelMethod(ModelAndViewContainer container) {
		for (ModelMethod modelMethod : this.modelMethods) {
			if (modelMethod.checkDependencies(container)) {
				if (logger.isTraceEnabled()) {
					logger.trace("Selected @ModelAttribute method " + modelMethod);
				}
				this.modelMethods.remove(modelMethod);
				return modelMethod;
			}
		}
		ModelMethod modelMethod = this.modelMethods.get(0);
		if (logger.isTraceEnabled()) {
			logger.trace("Selected @ModelAttribute method (not present: " +
					modelMethod.getUnresolvedDependencies(container)+ ") " + modelMethod);
		}
		this.modelMethods.remove(modelMethod);
		return modelMethod;
	}

	/**
	 * Find {@code @ModelAttribute} arguments also listed as {@code @SessionAttributes}.
	 */
	private List<String> findSessionAttributeArguments(HandlerMethod handlerMethod) {
		List<String> result = new ArrayList<String>();
		for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
			if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
				String name = getNameForParameter(parameter);
				Class<?> paramType = parameter.getParameterType();
				if (this.sessionAttributesHandler.isHandlerSessionAttribute(name, paramType)) {
					result.add(name);
				}
			}
		}
		return result;
	}

	/**
	 * Derives the model attribute name for a method parameter based on:
	 * <ol>
	 * <li>The parameter {@code @ModelAttribute} annotation value
	 * <li>The parameter type
	 * </ol>
	 * @return the derived name; never {@code null} or an empty string
	 */
	public static String getNameForParameter(MethodParameter parameter) {
		ModelAttribute ann = parameter.getParameterAnnotation(ModelAttribute.class);
		String name = (ann != null ? ann.value() : null);
		return StringUtils.hasText(name) ? name : Conventions.getVariableNameForParameter(parameter);
	}

	/**
	 * Derive the model attribute name for the given return value using one of:
	 * <ol>
	 * <li>The method {@code ModelAttribute} annotation value
	 * <li>The declared return type if it is more specific than {@code Object}
	 * <li>The actual return value type
	 * </ol>
	 * @param returnValue the value returned from a method invocation
	 * @param returnType the return type of the method
	 * @return the model name, never {@code null} nor empty
	 */
	public static String getNameForReturnValue(Object returnValue, MethodParameter returnType) {
		ModelAttribute ann = returnType.getMethodAnnotation(ModelAttribute.class);
		if (ann != null && StringUtils.hasText(ann.value())) {
			return ann.value();
		}
		else {
			Method method = returnType.getMethod();
			Class<?> containingClass = returnType.getContainingClass();
			Class<?> resolvedType = GenericTypeResolver.resolveReturnType(method, containingClass);
			return Conventions.getVariableNameForReturnType(method, resolvedType, returnValue);
		}
	}

	/**
	 * Promote model attributes listed as {@code @SessionAttributes} to the session.
	 * Add {@link BindingResult} attributes where necessary.
	 * @param request the current request
	 * @param container contains the model to update
	 * @throws Exception if creating BindingResult attributes fails
	 */
	public void updateModel(NativeWebRequest request, ModelAndViewContainer container) throws Exception {
		ModelMap defaultModel = container.getDefaultModel();
		if (container.getSessionStatus().isComplete()){
			this.sessionAttributesHandler.cleanupAttributes(request);
		}
		else {
			this.sessionAttributesHandler.storeAttributes(request, defaultModel);
		}
		if (!container.isRequestHandled() && container.getModel() == defaultModel) {
			updateBindingResult(request, defaultModel);
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
					WebDataBinder dataBinder = this.dataBinderFactory.createBinder(request, value, name);
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

		Class<?> attrType = (value != null ? value.getClass() : null);
		if (this.sessionAttributesHandler.isHandlerSessionAttribute(attributeName, attrType)) {
			return true;
		}

		return (value != null && !value.getClass().isArray() && !(value instanceof Collection) &&
				!(value instanceof Map) && !BeanUtils.isSimpleValueType(value.getClass()));
	}


	private static class ModelMethod {

		private final InvocableHandlerMethod handlerMethod;

		private final Set<String> dependencies = new HashSet<String>();

		public ModelMethod(InvocableHandlerMethod handlerMethod) {
			this.handlerMethod = handlerMethod;
			for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
				if (parameter.hasParameterAnnotation(ModelAttribute.class)) {
					this.dependencies.add(getNameForParameter(parameter));
				}
			}
		}

		public InvocableHandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public boolean checkDependencies(ModelAndViewContainer mavContainer) {
			for (String name : this.dependencies) {
				if (!mavContainer.containsAttribute(name)) {
					return false;
				}
			}
			return true;
		}

		public List<String> getUnresolvedDependencies(ModelAndViewContainer mavContainer) {
			List<String> result = new ArrayList<String>(this.dependencies.size());
			for (String name : this.dependencies) {
				if (!mavContainer.containsAttribute(name)) {
					result.add(name);
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return this.handlerMethod.getMethod().toGenericString();
		}
	}

}
