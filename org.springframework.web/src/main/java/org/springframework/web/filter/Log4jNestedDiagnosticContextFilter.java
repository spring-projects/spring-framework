/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.web.filter;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

/**
 * Request logging filter that adds the request log message to the Log4J
 * nested diagnostic context (NDC) before the request is processed,
 * removing it again after the request is processed.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.2.5
 * @see #setIncludeQueryString
 * @see #setBeforeMessagePrefix
 * @see #setBeforeMessageSuffix
 * @see #setAfterMessagePrefix
 * @see #setAfterMessageSuffix
 * @see org.apache.log4j.NDC#push(String)
 * @see org.apache.log4j.NDC#pop()
 */
public class Log4jNestedDiagnosticContextFilter extends AbstractRequestLoggingFilter {

	/** Logger available to subclasses */
	protected final Logger log4jLogger = Logger.getLogger(getClass());


	/**
	 * Logs the before-request message through Log4J and
	 * adds a message the Log4J NDC before the request is processed.
	 */
	@Override
	protected void beforeRequest(HttpServletRequest request, String message) {
		if (log4jLogger.isDebugEnabled()) {
			log4jLogger.debug(message);
		}
		NDC.push(getNestedDiagnosticContextMessage(request));
	}

	/**
	 * Determine the message to be pushed onto the Log4J nested diagnostic context.
	 * <p>Default is a plain request log message without prefix or suffix.
	 * @param request current HTTP request
	 * @return the message to be pushed onto the Log4J NDC
	 * @see #createMessage
	 */
	protected String getNestedDiagnosticContextMessage(HttpServletRequest request) {
		return createMessage(request, "", "");
	}

	/**
	 * Removes the log message from the Log4J NDC after the request is processed
	 * and logs the after-request message through Log4J.
	 */
	@Override
	protected void afterRequest(HttpServletRequest request, String message) {
		NDC.pop();
		if (NDC.getDepth() == 0) {
			NDC.remove();
		}
		if (log4jLogger.isDebugEnabled()) {
			log4jLogger.debug(message);
		}
	}

}
