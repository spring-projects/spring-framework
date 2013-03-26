/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

/**
 * Implementation of the {@link org.springframework.web.servlet.HandlerExceptionResolver HandlerExceptionResolver}
 * interface that uses the {@link ResponseStatus @ResponseStatus} annotation to map exceptions to HTTP status codes.
 *
 * <p>This exception resolver is enabled by default in the {@link org.springframework.web.servlet.DispatcherServlet}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 3.0
 */
public class ResponseStatusExceptionResolver extends AbstractHandlerExceptionResolver implements MessageSourceAware {

	private MessageSource messageSource;


	public void setMessageSource(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response,
			Object handler, Exception ex) {

		ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
		if (responseStatus != null) {
			try {
				return resolveResponseStatus(responseStatus, request, response, handler, ex);
			}
			catch (Exception resolveEx) {
				logger.warn("Handling of @ResponseStatus resulted in Exception", resolveEx);
			}
		}
		return null;
	}

	/**
	 * Template method that handles {@link ResponseStatus @ResponseStatus} annotation. <p>Default implementation send a
	 * response error using {@link HttpServletResponse#sendError(int)}, or {@link HttpServletResponse#sendError(int,
	 * String)} if the annotation has a {@linkplain ResponseStatus#reason() reason}. Returns an empty ModelAndView.
	 * @param responseStatus the annotation
	 * @param request current HTTP request
	 * @param response current HTTP response
	 * @param handler the executed handler, or {@code null} if none chosen at the time of the exception
	 * (for example, if multipart resolution failed)
	 * @param ex the exception that got thrown during handler execution
	 * @return a corresponding ModelAndView to forward to, or {@code null} for default processing
	 */
	protected ModelAndView resolveResponseStatus(ResponseStatus responseStatus, HttpServletRequest request,
			HttpServletResponse response, Object handler, Exception ex) throws Exception {

		int statusCode = responseStatus.value().value();
		String reason = responseStatus.reason();
		if (this.messageSource != null) {
			reason = this.messageSource.getMessage(reason, null, reason, LocaleContextHolder.getLocale());
		}
		if (!StringUtils.hasLength(reason)) {
			response.sendError(statusCode);
		}
		else {
			response.sendError(statusCode, reason);
		}
		return new ModelAndView();
	}

}
