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

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * An implementation of {@link HandlerMethodArgumentResolver} that resolves {@link Errors} method parameters.
 * Such parameters must be preceded by {@link ModelAttribute} parameters as described in {@link RequestMapping}.
 * 
 * @author Rossen Stoyanchev
 */
public class ErrorsMethodArgumentResolver implements HandlerMethodArgumentResolver {

	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> paramType = parameter.getParameterType();
		return Errors.class.isAssignableFrom(paramType);
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public Object resolveArgument(MethodParameter parameter, 
								  ModelAndViewContainer mavContainer, 
								  NativeWebRequest webRequest,
								  WebDataBinderFactory binderFactory) throws Exception {
		ModelMap model = mavContainer.getModel();
		if (model.size() > 0) {
			List<String> keys = new ArrayList<String>(model.keySet());
			String lastKey = keys.get(model.size()-1);
			if (isBindingResultKey(lastKey)) {
				return model.get(lastKey);
			}
		}

		throw new IllegalStateException("Errors/BindingResult argument declared "
				+ "without preceding model attribute. Check your handler method signature!");
	}

	private boolean isBindingResultKey(String key) {
		return key.startsWith(BindingResult.MODEL_KEY_PREFIX);
	}

}
