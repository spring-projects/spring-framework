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
 * Extends {@link InvocableHandlerMethod} with the ability to handle the value returned from the method through
 * a registered {@link HandlerMethodArgumentResolver} that supports the given return value type.
 * Return value handling may include writing to the response or updating the {@link ModelAndViewContainer} structure.
 *
 * <p>If the underlying method has a {@link ResponseStatus} instruction, the status on the response is set
 * accordingly after the method is invoked but before the return value is handled.
 *
 * @author Rossen Stoyanchev
 * @since 3.1
 * @see #invokeAndHandle(NativeWebRequest, ModelAndViewContainer, Object...)
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
	 * Invokes the method and handles the return value through a registered {@link HandlerMethodReturnValueHandler}.
	 * <p>Return value handling may be skipped entirely when the method returns {@code null} (also possibly due
	 * to a {@code void} return type) and one of the following additional conditions is true:
	 * <ul>
	 * <li>A {@link HandlerMethodArgumentResolver} has set the {@link ModelAndViewContainer#setRequestHandled(boolean)}
	 * flag to {@code false} -- e.g. method arguments providing access to the response.
	 * <li>The request qualifies as "not modified" as defined in {@link ServletWebRequest#checkNotModified(long)}
	 * and {@link ServletWebRequest#checkNotModified(String)}. In this case a response with "not modified" response
	 * headers will be automatically generated without the need for return value handling.
	 * <li>The status on the response is set due to a @{@link ResponseStatus} instruction.
	 * </ul>
	 * <p>After the return value is handled, callers of this method can use the {@link ModelAndViewContainer}
	 * to gain access to model attributes, view selection choices, and to check if view resolution is even needed.
	 *
	 * @param request the current request
	 * @param mavContainer the {@link ModelAndViewContainer} for the current request
	 * @param providedArgs argument values to try to use without the need for view resolution
	 */
	public final void invokeAndHandle(
			NativeWebRequest request, ModelAndViewContainer mavContainer,
			Object... providedArgs) throws Exception {

		Object returnValue = invokeForRequest(request, mavContainer, providedArgs);

		setResponseStatus((ServletWebRequest) request);

		if (returnValue == null) {
			if (isRequestNotModified(request) || hasResponseStatus() || mavContainer.isRequestHandled()) {
				mavContainer.setRequestHandled(true);
				return;
			}
		}

		mavContainer.setRequestHandled(false);

		try {
			returnValueHandlers.handleReturnValue(returnValue, getReturnValueType(returnValue), mavContainer, request);
		} catch (Exception ex) {
			if (logger.isTraceEnabled()) {
				logger.trace(getReturnValueHandlingErrorMessage("Error handling return value", returnValue), ex);
			}
			throw ex;
		}
	}

	private String getReturnValueHandlingErrorMessage(String message, Object returnValue) {
		StringBuilder sb = new StringBuilder(message);
		if (returnValue != null) {
			sb.append(" [type=" + returnValue.getClass().getName() + "] ");
		}
		sb.append("[value=" + returnValue + "]");
		return getDetailedErrorMessage(sb.toString());
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
				webRequest.getResponse().setStatus(this.responseStatus.value());
			}

			// to be picked up by the RedirectView
			webRequest.getRequest().setAttribute(View.RESPONSE_STATUS_ATTRIBUTE, this.responseStatus);
		}
	}

	/**
	 * Does the given request qualify as "not modified"?
	 * @see ServletWebRequest#checkNotModified(long)
	 * @see ServletWebRequest#checkNotModified(String)
	 */
	private boolean isRequestNotModified(NativeWebRequest request) {
		return ((ServletWebRequest) request).isNotModified();
	}

	/**
	 * Does this method have the response status instruction?
	 */
	private boolean hasResponseStatus() {
		return responseStatus != null;
	}
}
