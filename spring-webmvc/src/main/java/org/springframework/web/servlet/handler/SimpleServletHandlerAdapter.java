/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.handler;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * Adapter to use the Servlet interface with the generic DispatcherServlet.
 * Calls the Servlet's {@code service} method to handle a request.
 *
 * <p>Last-modified checking is not explicitly supported: This is typically
 * handled by the Servlet implementation itself (usually deriving from
 * the HttpServlet base class).
 *
 * <p>This adapter is not activated by default; it needs to be defined as a
 * bean in the DispatcherServlet context. It will automatically apply to
 * mapped handler beans that implement the Servlet interface then.
 *
 * <p>Note that Servlet instances defined as bean will not receive initialization
 * and destruction callbacks, unless a special post-processor such as
 * SimpleServletPostProcessor is defined in the DispatcherServlet context.
 *
 * <p><b>Alternatively, consider wrapping a Servlet with Spring's
 * ServletWrappingController.</b> This is particularly appropriate for
 * existing Servlet classes, allowing to specify Servlet initialization
 * parameters etc.
 *
 * @author Juergen Hoeller
 * @since 1.1.5
 * @see jakarta.servlet.Servlet
 * @see jakarta.servlet.http.HttpServlet
 * @see SimpleServletPostProcessor
 * @see org.springframework.web.servlet.mvc.ServletWrappingController
 */
public class SimpleServletHandlerAdapter implements HandlerAdapter {

	@Override
	public boolean supports(Object handler) {
		return (handler instanceof Servlet);
	}

	@Override
	public @Nullable ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		((Servlet) handler).service(request, response);
		return null;
	}

}
