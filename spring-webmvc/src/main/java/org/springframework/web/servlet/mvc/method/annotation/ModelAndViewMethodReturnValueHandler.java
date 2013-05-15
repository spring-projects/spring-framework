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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.SmartView;
import org.springframework.web.servlet.View;

/**
 * Handles return values of type {@link ModelAndView} copying view and model
 * information to the {@link ModelAndViewContainer}.
 *
 * <p>If the return value is {@code null}, the
 * {@link ModelAndViewContainer#setRequestHandled(boolean)} flag is set to
 * {@code false} to indicate the request was handled directly.
 *
 * <p>A {@link ModelAndView} return type has a set purpose. Therefore this
 * handler should be configured ahead of handlers that support any return
 * value type annotated with {@code @ModelAttribute} or {@code @ResponseBody}
 * to ensure they don't take over.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ModelAndViewMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return ModelAndView.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(
			Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest)
			throws Exception {

		if (returnValue == null) {
			mavContainer.setRequestHandled(true);
			return;
		}

		ModelAndView mav = (ModelAndView) returnValue;
		if (mav.isReference()) {
			String viewName = mav.getViewName();
			mavContainer.setViewName(viewName);
			if (viewName != null && viewName.startsWith("redirect:")) {
				mavContainer.setRedirectModelScenario(true);
			}
		}
		else {
			View view = mav.getView();
			mavContainer.setView(view);
			if (view instanceof SmartView) {
				if (((SmartView) view).isRedirectView()) {
					mavContainer.setRedirectModelScenario(true);
				}
			}
		}
		mavContainer.addAllAttributes(mav.getModel());
	}

}
