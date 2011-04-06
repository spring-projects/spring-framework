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

package org.springframework.web.servlet.mvc.method.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;

/**
 * Handles {@link View} and view name return values.
 *  
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return (View.class.isAssignableFrom(paramType) || (String.class.equals(paramType)));
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public void handleReturnValue(Object returnValue, 
								  MethodParameter returnType, 
								  ModelAndViewContainer mavContainer, 
								  NativeWebRequest webRequest) throws Exception {
		if (returnValue == null) {
			return;
		}
		if (returnValue instanceof String) {
			mavContainer.setViewName((String) returnValue);
		}
		else if (returnValue instanceof View){
			mavContainer.setView(returnValue);
		}
		else {
			// should not happen
			throw new UnsupportedOperationException();
		}
	}

}
