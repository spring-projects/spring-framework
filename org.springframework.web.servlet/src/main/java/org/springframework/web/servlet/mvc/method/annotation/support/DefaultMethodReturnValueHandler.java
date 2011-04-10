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
 * A catch-all {@link HandlerMethodReturnValueHandler} to handle return values not handled by any other return 
 * value handler. 
 * 
 * <p>This handler should always be last in the order as {@link #supportsReturnType(MethodParameter)} always returns
 * {@code true}. An attempt is made to handle the return value through a custom {@link ModelAndViewResolver}s or 
 * otherwise by treating it as a single model attribute.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class DefaultMethodReturnValueHandler implements HandlerMethodReturnValueHandler {
	
	private final ModelAndViewResolver[] customModelAndViewResolvers;

	public DefaultMethodReturnValueHandler(ModelAndViewResolver[] customResolvers) {
		this.customModelAndViewResolvers = (customResolvers != null) ? customResolvers : new ModelAndViewResolver[] {};
	}

	public boolean supportsReturnType(MethodParameter returnType) {
		return true;
	}

	public boolean usesResponseArgument(MethodParameter parameter) {
		return false;
	}

	public void handleReturnValue(Object returnValue, 
								  MethodParameter returnType, 
								  ModelAndViewContainer mavContainer, 
								  NativeWebRequest webRequest) throws Exception {

		for (ModelAndViewResolver resolver : this.customModelAndViewResolvers) {
			Class<?> handlerType = returnType.getDeclaringClass();
			Method method = returnType.getMethod();
			ExtendedModelMap extModel = (ExtendedModelMap) mavContainer.getModel();
			ModelAndView mav = resolver.resolveModelAndView(method, handlerType, returnValue, extModel, webRequest);
			if (mav != ModelAndViewResolver.UNRESOLVED) {
				mavContainer.setView(mav.getView());
				mavContainer.setViewName(mav.getViewName());
				mavContainer.addAllAttributes(mav.getModel());
				return;
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
			// should not happen
			throw new UnsupportedOperationException();
		}
	}

}
