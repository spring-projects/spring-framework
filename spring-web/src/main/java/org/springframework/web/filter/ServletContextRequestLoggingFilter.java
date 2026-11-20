/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.filter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Simple request logging filter that writes the request URI
 * (and optionally the query string) to the ServletContext log.
 *
 * @author Juergen Hoeller
 * @since 1.2.5
 * @see #setIncludeQueryString
 * @see #setBeforeMessagePrefix
 * @see #setBeforeMessageSuffix
 * @see #setAfterMessagePrefix
 * @see #setAfterMessageSuffix
 * @see jakarta.servlet.ServletContext#log(String)
 */
public class ServletContextRequestLoggingFilter extends AbstractRequestLoggingFilter {

	/**
	 * Writes a log message before the request is processed.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		getServletContext().log(message);
	}

	/**
	 * Writes a log message after the request is processed.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		getServletContext().log(message);
	}

}
