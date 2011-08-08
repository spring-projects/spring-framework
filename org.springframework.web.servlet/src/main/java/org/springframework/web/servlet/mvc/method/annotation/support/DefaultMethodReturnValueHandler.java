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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ModelFactory;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * Attempts to handle return value types not recognized by any other {@link HandlerMethodReturnValueHandler}. 
 * Intended to be used as the last of a list of registered handlers as {@link #supportsReturnType(MethodParameter)}
 * always returns {@code true}.
 * <p>Handling takes place in the following order:
 * <ul>
 * <li>Iterate over the list of {@link ModelAndViewResolver}s provided to the constructor of this class looking 
 * for a return value that isn't {@link ModelAndViewResolver#UNRESOLVED}. 
 * <li>If the return value is not a simple type it is treated as a single model attribute to be added to the model 
 * with a name derived from its type.
 * </ul>
 * <p>Note that {@link ModelAndViewResolver} is supported for backwards compatibility. Since the only way to check 
 * if it supports a return value type is to try to resolve the return value, a {@link ModelAndViewResolver} can
 * only be invoked from here after no other {@link HandlerMethodReturnValueHandler} has recognized the return 
 * value. To avoid this limitation change the {@link ModelAndViewResolver} to implement 
 * {@link HandlerMethodReturnValueHandler} instead.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	
	private final List<ModelAndViewResolver> mavResolvers;

	/**
	 * Create a {@link DefaultMethodReturnValueHandler} instance without {@link ModelAndViewResolver}s.
	 */
	public DefaultMethodReturnValueHandler() {
		this(null);
	}

	/**
	 * Create a {@link DefaultMethodReturnValueHandler} with a list of {@link ModelAndViewResolver}s.
	 */
	public DefaultMethodReturnValueHandler(List<ModelAndViewResolver> mavResolvers) {
		this.mavResolvers = mavResolvers;
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	public void handleReturnValue(Object returnValue, 
								  MethodParameter returnType, 
								  ModelAndViewContainer mavContainer, 
								  NativeWebRequest request) throws Exception {

		if (mavResolvers != null) {
			for (ModelAndViewResolver resolver : mavResolvers) {
				Class<?> handlerType = returnType.getDeclaringClass();
				Method method = returnType.getMethod();
				ExtendedModelMap model = (ExtendedModelMap) mavContainer.getModel();
				ModelAndView mav = resolver.resolveModelAndView(method, handlerType, returnValue, model, request);
				if (mav != ModelAndViewResolver.UNRESOLVED) {
					mavContainer.addAllAttributes(mav.getModel());
					mavContainer.setViewName(mav.getViewName());
					if (!mav.isReference()) {
						mavContainer.setView(mav.getView());
					}
					return;
				}
			}
		}

		if (returnValue == null) {
			return;
		}
		else if (!BeanUtils.isSimpleProperty(returnValue.getClass())) {
			String name = ModelFactory.getNameForReturnValue(returnValue, returnType);
			mavContainer.addAttribute(name, returnValue);
			return;
		}
		else {
			// should not happen..
			Method method = returnType.getMethod();
			String returnTypeName = returnType.getParameterType().getName();
			throw new UnsupportedOperationException("Unknown return type: " + returnTypeName + " in method: " + method);
		}
	}

}