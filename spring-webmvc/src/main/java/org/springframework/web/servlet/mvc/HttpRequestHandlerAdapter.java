/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adapter to use the plain {@link org.springframework.web.HttpRequestHandler}
 * interface with the generic {@link org.springframework.web.servlet.DispatcherServlet}.
 *
 * <p>This is an SPI class, not used directly by application code.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.web.servlet.DispatcherServlet
 * @see org.springframework.web.HttpRequestHandler
 * @see SimpleControllerHandlerAdapter
 */
public class HttpRequestHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof HttpRequestHandler);
	}

	@Override
	public @Nullable ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		((HttpRequestHandler) handler).handleRequest(request, response);
		return null;
	}

}
