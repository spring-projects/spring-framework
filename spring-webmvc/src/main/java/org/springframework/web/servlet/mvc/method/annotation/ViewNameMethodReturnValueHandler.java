/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.RequestToViewNameTranslator;

/**
 * Handles return values of types {@code void} and {@code String} interpreting
 * them as view name reference.
 *
 * <p>A {@code null} return value, either due to a {@code void} return type or
 * as the actual return value is left as-is allowing the configured
 * {@link RequestToViewNameTranslator} to select a view name by convention.
 *
 * <p>A String return value can be interpreted in more than one ways depending
 * on the presence of annotations like {@code @ModelAttribute} or
 * {@code @ResponseBody}. Therefore this handler should be configured after
 * the handlers that support these annotations.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ViewNameMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> paramType = returnType.getParameterType();
		return (void.class.equals(paramType) || String.class.equals(paramType));
	}

	public void handleReturnValue(
			Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws Exception {

		if (returnValue == null) {
			return;
		}
		else if (returnValue instanceof String) {
			String viewName = (String) returnValue;
			mavContainer.setViewName(viewName);
			if (isRedirectViewName(viewName)) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		else {
			// should not happen
			throw new UnsupportedOperationException("Unexpected return type: " +
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

}
