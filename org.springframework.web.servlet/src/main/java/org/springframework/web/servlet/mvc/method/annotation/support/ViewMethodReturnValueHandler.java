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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * Handles return values that are of type {@code void}, {@code String} (i.e. 
 * logical view name), or {@link View}.
 *
 * <p>A {@code null} return value, either due to a void return type or as the
 * actual value returned from a method is left unhandled, leaving it to the 
 * configured {@link RequestToViewNameTranslator} to resolve the request to 
 * an actual view name. 
 *
 * <p>Since a {@link String} return value may be handled in combination with 
 * method annotations such as @{@link ModelAttribute} or @{@link ResponseBody},
 * this handler should be ordered after return value handlers that support 
 * method annotations.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> type = returnType.getParameterType();
		return (void.class.equals(type) || String.class.equals(type) || View.class.isAssignableFrom(type));
	}

	public void handleReturnValue(Object returnValue,
								  MethodParameter returnType,
								  ModelAndViewContainer mavContainer,
								  NativeWebRequest webRequest) throws Exception {
		if (returnValue == null) {
			return;
		}
		if (returnValue instanceof String) {
			String viewName = (String) returnValue;
			mavContainer.setViewName(viewName);
			if (isRedirectViewName(viewName)) {
				mavContainer.setUseRedirectModel();
			}
		}
		else if (returnValue instanceof View){
			View view = (View) returnValue;
			mavContainer.setView(view);
			if (isRedirectView(view)) {	
				mavContainer.setUseRedirectModel();
			}
		}
		else {
			// should not happen
			throw new UnsupportedOperationException("Unknown return type: " + 
					returnType.getParameterType().getName() + " in method: " + returnType.getMethod());
		}
	}

	/**
	 * Whether the given view name is a redirect view reference.
	 * @param viewName the view name to check, never {@code null}
	 * @return "true" if the given view name is recognized as a redirect view 
	 * reference; "false" otherwise.
	 */
	protected boolean isRedirectViewName(String viewName) {
		return viewName.startsWith("redirect:");
	}

	/**
	 * Whether the given View instance is a redirect view.
	 * @param view a view instance, never {@code null}
	 * @return "true" if the given view is recognized as a redirect View; 
	 * "false" otherwise.
	 */
	protected boolean isRedirectView(View view) {
		if (SmartView.class.isAssignableFrom(view.getClass())) {
			return ((SmartView) view).isRedirectView();
		}
		else {
			return false;
		}
	}

}
