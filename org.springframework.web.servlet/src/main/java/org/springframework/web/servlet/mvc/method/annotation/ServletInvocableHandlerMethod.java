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

package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.lang.reflect.Method;

import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * Extends {@link InvocableHandlerMethod} with the ability to handle the return value of the invocation
 * resulting in a {@link ModelAndView} according to the {@link HandlerAdapter} contract.
 * 
 * @author Rossen Stoyanchev
 * @since 3.1
 */
public class ServletInvocableHandlerMethod extends InvocableHandlerMethod {

	private HttpStatus responseStatus;
	
	private String responseReason;
	
	private HandlerMethodReturnValueHandlerComposite returnValueHandlers;

	public void setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite returnValueHandlers) {
		this.returnValueHandlers = returnValueHandlers;
	}

	/**
	 * Creates a {@link ServletInvocableHandlerMethod} instance with the given bean and method.
	 * @param handler the object handler
	 * @param method the method
	 */
	public ServletInvocableHandlerMethod(Object handler, Method method) {
		super(handler, method);

		ResponseStatus annotation = getMethodAnnotation(ResponseStatus.class);
		if (annotation != null) {
			this.responseStatus = annotation.value();
			this.responseReason = annotation.reason();

		}
	}

	/**
	 * Invokes the method via {@link #invokeForRequest(NativeWebRequest, ModelMap, Object...)} and also handles the 
	 * return value by invoking one of the {@link HandlerMethodReturnValueHandler} instances registered via 
	 * {@link #setHandlerMethodReturnValueHandlers(HandlerMethodReturnValueHandlerComposite)}. 
	 * If the method is annotated with {@link SessionStatus} the response status will be set.
	 * @param request the current request
	 * @param model the model used throughout the current request
	 * @param providedArgs argument values to use as-is if they match to a method parameter's type
	 * @return ModelAndView object with the name of the view and the required model data, or <code>null</code> 
	 * if the response was handled
	 */
	public final ModelAndView invokeAndHandle(NativeWebRequest request, 
											  ModelMap model, 
											  Object... providedArgs)  throws Exception {
		
		if (!returnValueHandlers.supportsReturnType(getReturnType())) {
			throw new IllegalStateException("No suitable HandlerMethodReturnValueHandler for method " + toString());
		}

		Object returnValue = invokeForRequest(request, model, providedArgs);

		setResponseStatus((ServletWebRequest) request);

		if (returnValue == null && (isRequestNotModified(request) || usesResponseArgument())) {
			return null;
		}
		
		ModelAndViewContainer mavContainer = new ModelAndViewContainer(model);
		returnValueHandlers.handleReturnValue(returnValue, getReturnType(), mavContainer, request);

		return getModelAndView(request, mavContainer, returnValue);
	}

	/**
	 * Set the response status according to the {@link ResponseStatus} annotation.
	 */
	private void setResponseStatus(ServletWebRequest webRequest) throws IOException {
		if (this.responseStatus != null) {
			if (StringUtils.hasText(this.responseReason)) {
				webRequest.getResponse().sendError(this.responseStatus.value(), this.responseReason);
			}
			else {
				webRequest.getResponse().sendError(this.responseStatus.value());
			}

			// to be picked up by the RedirectView
			webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, this.responseStatus);
		}
	}

	/**
	 * Create a {@link ModelAndView} from a {@link ModelAndViewContainer}.
	 */
	private ModelAndView getModelAndView(NativeWebRequest request, 
										 ModelAndViewContainer mavContainer,
										 Object returnValue) {
		if (returnValueHandlerUsesResponseArgument()) {
			return null;
		}
		else {
			ModelAndView mav = new ModelAndView().addAllObjects(mavContainer.getModel());
			mav.setViewName(mavContainer.getViewName());
			if (mavContainer.getView() != null) {
				mav.setView((View) mavContainer.getView());
			} 
			return mav;			
		}
	}

	/**
	 * Check whether the request qualifies as not modified... 
	 * TODO: document fully including sample user code
	 */
	private boolean isRequestNotModified(NativeWebRequest request) {
		ServletWebRequest servletRequest = (ServletWebRequest) request;
		return (servletRequest.isNotModified() || (responseStatus != null) || usesResponseArgument());
	}
	
	protected boolean usesResponseArgument() {
		return (super.usesResponseArgument() || returnValueHandlerUsesResponseArgument() || (responseStatus != null));
	}

	private boolean returnValueHandlerUsesResponseArgument() {
		return returnValueHandlers.usesResponseArgument(getReturnType());
	}
}
