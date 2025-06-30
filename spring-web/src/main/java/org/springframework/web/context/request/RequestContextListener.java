/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.context.request;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.i18n.LocaleContextHolder;

/**
 * Servlet listener that exposes the request to the current thread,
 * through both {@link org.springframework.context.i18n.LocaleContextHolder} and
 * {@link RequestContextHolder}. To be registered as listener in {@code web.xml}.
 *
 * <p>Alternatively, Spring's {@link org.springframework.web.filter.RequestContextFilter}
 * and Spring's {@link org.springframework.web.servlet.DispatcherServlet} also expose
 * the same request context to the current thread. In contrast to this listener,
 * advanced options are available there (for example, "threadContextInheritable").
 *
 * <p>This listener is mainly for use with third-party servlets, for example, the JSF FacesServlet.
 * Within Spring's own web support, DispatcherServlet's processing is perfectly sufficient.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see jakarta.servlet.ServletRequestListener
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see RequestContextHolder
 * @see org.springframework.web.filter.RequestContextFilter
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class RequestContextListener implements ServletRequestListener {

	private static final String REQUEST_ATTRIBUTES_ATTRIBUTE =
			RequestContextListener.class.getName() + ".REQUEST_ATTRIBUTES";


	@Override
	public void requestInitialized(ServletRequestEvent requestEvent) {
		if (!(requestEvent.getServletRequest() instanceof HttpServletRequest request)) {
			throw new IllegalArgumentException(
					"Request is not an HttpServletRequest: " + requestEvent.getServletRequest());
		}
		ServletRequestAttributes attributes = new ServletRequestAttributes(request);
		request.setAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE, attributes);
		LocaleContextHolder.setLocale(request.getLocale());
		RequestContextHolder.setRequestAttributes(attributes);
	}

	@Override
	public void requestDestroyed(ServletRequestEvent requestEvent) {
		ServletRequestAttributes attributes = null;
		Object reqAttr = requestEvent.getServletRequest().getAttribute(REQUEST_ATTRIBUTES_ATTRIBUTE);
		if (reqAttr instanceof ServletRequestAttributes servletRequestAttributes) {
			attributes = servletRequestAttributes;
		}
		RequestAttributes threadAttributes = RequestContextHolder.getRequestAttributes();
		if (threadAttributes != null) {
			// We're assumably within the original request thread...
			LocaleContextHolder.resetLocaleContext();
			RequestContextHolder.resetRequestAttributes();
			if (attributes == null && threadAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
				attributes = servletRequestAttributes;
			}
		}
		if (attributes != null) {
			attributes.requestCompleted();
		}
	}

}
