/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Handles {@link HttpHeaders} return values.
 *
 * @author Stephane Nicoll
 * @since 4.0.1
 */
public class HttpHeadersReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return HttpHeaders.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	@SuppressWarnings("resource")
	public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,
			ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {

		mavContainer.setRequestHandled(true);

		Assert.state(returnValue instanceof HttpHeaders, "HttpHeaders expected");
		HttpHeaders headers = (HttpHeaders) returnValue;

		if (!headers.isEmpty()) {
			HttpServletResponse servletResponse = webRequest.getNativeResponse(HttpServletResponse.class);
			Assert.state(servletResponse != null, "No HttpServletResponse");
			ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(servletResponse);
			outputMessage.getHeaders().putAll(headers);
			outputMessage.flush();  // flush headers
		}
	}

}
