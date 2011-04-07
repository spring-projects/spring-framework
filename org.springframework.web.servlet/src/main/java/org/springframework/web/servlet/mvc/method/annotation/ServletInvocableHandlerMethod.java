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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;

/**
 * Extends {@link InvocableHandlerMethod} with the ability to handle the return value through registered 
 * {@link HandlerMethodArgumentResolver}s. If the handler method is annotated with {@link ResponseStatus}, 
 * the  status on the response is set accordingly after method invocation but before return value handling.
 * 
 * <p>Return value handling may be skipped entirely if the handler method returns a {@code null} (or is a 
 * {@code void} method) and one of the following other conditions is true:
 * <ul>
 * <li>One of the {@link HandlerMethodArgumentResolver}s set the {@link ModelAndViewContainer#setResolveView(boolean)} 
 * flag to {@code false}. This is the case when a method argument allows the handler method access to the response.
 * <li>The request qualifies as being not modified according to {@link ServletWebRequest#isNotModified()}.
 * This is used in conjunction with a "Last-Modified" header or ETag.
 * <li>The status on the response was set as a result of a {@link ResponseStatus} annotation
 * </ul>
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
	 * Invokes the method and handles the return value through registered {@link HandlerMethodReturnValueHandler}s. 
	 * If the handler method is annotated with {@link ResponseStatus}, the status on the response is set accordingly 
	 * after method invocation but before return value handling.
	 * <p>Return value handling may be skipped entirely if the handler method returns a {@code null} (or is a 
	 * {@code void} method) and one of the following other conditions is true:
	 * <ul>
	 * <li>One of the {@link HandlerMethodArgumentResolver}s set the {@link ModelAndViewContainer#setResolveView(boolean)} 
	 * flag to {@code false}. This is the case when a method argument allows the handler method access to the response.
	 * <li>The request qualifies as being not modified according to {@link ServletWebRequest#isNotModified()}.
	 * This is used in conjunction with a "Last-Modified" header or ETag.
	 * <li>The status on the response was set as a result of a {@link ResponseStatus} annotation
	 * </ul>
	 * <p>After the call, use the {@link ModelAndViewContainer} parameter to access model attributes and view selection
	 * and to determine if view resolution is needed.
	 * 
	 * @param request the current request
	 * @param mavContainer the {@link ModelAndViewContainer} for the current request
	 * @param providedArgs argument values to try to use without the need for view resolution
	 */
	public final void invokeAndHandle(NativeWebRequest request, 
									  ModelAndViewContainer mavContainer, 
									  Object...providedArgs) throws Exception {

		if (!returnValueHandlers.supportsReturnType(getReturnType())) {
			throw new IllegalStateException("No suitable HandlerMethodReturnValueHandler for method " + toString());
		}

		Object returnValue = invokeForRequest(request, mavContainer, providedArgs);

		setResponseStatus((ServletWebRequest) request);

		if (returnValue == null) {
			if (isRequestNotModified(request) || hasResponseStatus() || !mavContainer.isResolveView()) {
				mavContainer.setResolveView(false);
				return;
			}
		}
		
		mavContainer.setResolveView(true);

		returnValueHandlers.handleReturnValue(returnValue, getReturnType(), mavContainer, request);
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
	 * Does the request qualify as not modified?
	 */
	private boolean isRequestNotModified(NativeWebRequest request) {
		return ((ServletWebRequest) request).isNotModified();
	}

	/**
	 * Does the method set the response status?
	 */
	private boolean hasResponseStatus() {
		return responseStatus != null;
	}

}