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

import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link Model} and {@link Map} method parameters. 
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelMethodProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return Model.class.isAssignableFrom(paramType) || Map.class.isAssignableFrom(paramType);
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public Object resolveArgument(MethodParameter parameter, 
								  ModelMap model, 
								  NativeWebRequest webRequest,
								  WebDataBinderFactory binderFactory) throws Exception {
		Class<?> paramType = parameter.getParameterType();
		if (Model.class.isAssignableFrom(paramType)) {
			return model;
		}
		else if (Map.class.isAssignableFrom(paramType)) {
			return model;
		}
		
		// should not happen
		throw new UnsupportedOperationException();
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		boolean hasModelAttr = returnType.getMethodAnnotation(ModelAttribute.class) != null;
		
		return (Model.class.isAssignableFrom(paramType) 
				|| (Map.class.isAssignableFrom(paramType) && !hasModelAttr));
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
			mavContainer.addModelAttributes((Model) returnValue);
		}
		else if (returnValue instanceof Map){
			mavContainer.addModelAttributes((Map) returnValue);
		}
		else {
			// should not happen
			throw new UnsupportedOperationException();
		}
	}

}
