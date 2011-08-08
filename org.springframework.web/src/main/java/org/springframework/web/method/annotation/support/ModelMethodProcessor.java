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

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Map} and {@link Model} method arguments. 
 * 
 * <p>Handles {@link Model} return values adding their attributes to the {@link ModelAndViewContainer}. 
 * Handles {@link Map} return values in the same way as long as the method does not have an @{@link ModelAttribute}.
 * If the method does have an @{@link ModelAttribute}, it is assumed the returned {@link Map} is a model attribute
 * and not a model.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return Model.class.isAssignableFrom(paramType) || Map.class.isAssignableFrom(paramType);
	}

	public Object resolveArgument(MethodParameter parameter, 
								  ModelAndViewContainer mavContainer, 
								  NativeWebRequest webRequest,
								  WebDataBinderFactory binderFactory) throws Exception {
		return mavContainer.getModel();
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		boolean hasModelAttr = returnType.getMethodAnnotation(ModelAttribute.class) != null;
		return (Model.class.isAssignableFrom(paramType) || (Map.class.isAssignableFrom(paramType) && !hasModelAttr));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void handleReturnValue(Object returnValue, 
								  MethodParameter returnType, 
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest) throws Exception {
		if (returnValue == null) {
			return;
		}
		if (returnValue instanceof Model) {
			mavContainer.addAllAttributes(((Model) returnValue).asMap());
		}
		else if (returnValue instanceof Map){
			mavContainer.addAllAttributes((Map) returnValue);
		}
		else {
			// should not happen
			Method method = returnType.getMethod();
			String returnTypeName = returnType.getParameterType().getName();
			throw new UnsupportedOperationException("Unknown return type: " + returnTypeName + " in method: " + method);
		}
	}
}