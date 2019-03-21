/*
 * Copyright 2002-2012 the original author or authors.
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

import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import org.springframework.ui.ModelMap;

/**
 * Request logging interceptor that adds a request context message to the
 * Log4J nested diagnostic context (NDC) before the request is processed,
 * removing it again after the request is processed.
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see org.apache.log4j.NDC#push(String)
 * @see org.apache.log4j.NDC#pop()
 * @deprecated as of Spring 4.2.1, in favor of Apache Log4j 2
 * (following Apache's EOL declaration for log4j 1.x)
 */
@Deprecated
public class Log4jNestedDiagnosticContextInterceptor implements AsyncWebRequestInterceptor {

	/** Logger available to subclasses */
	protected final Logger log4jLogger = Logger.getLogger(getClass());

	private boolean includeClientInfo = false;


	/**
	 * Set whether or not the session id and user name should be included
	 * in the log message.
	 */
	public void setIncludeClientInfo(boolean includeClientInfo) {
		this.includeClientInfo = includeClientInfo;
	}

	/**
	 * Return whether or not the session id and user name should be included
	 * in the log message.
	 */
	protected boolean isIncludeClientInfo() {
		return this.includeClientInfo;
	}


	/**
	 * Adds a message the Log4J NDC before the request is processed.
	 */
	@Override
	public void preHandle(WebRequest request) throws Exception {
		NDC.push(getNestedDiagnosticContextMessage(request));
	}

	/**
	 * Determine the message to be pushed onto the Log4J nested diagnostic context.
	 * <p>Default is the request object's {@code getDescription} result.
	 * @param request current HTTP request
	 * @return the message to be pushed onto the Log4J NDC
	 * @see WebRequest#getDescription
	 * @see #isIncludeClientInfo()
	 */
	protected String getNestedDiagnosticContextMessage(WebRequest request) {
		return request.getDescription(isIncludeClientInfo());
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) throws Exception {
	}

	/**
	 * Removes the log message from the Log4J NDC after the request is processed.
	 */
	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws Exception {
		NDC.pop();
		if (NDC.getDepth() == 0) {
			NDC.remove();
		}
	}

	/**
	 * Removes the log message from the Log4J NDC when the processing thread is
	 * exited after the start of asynchronous request handling.
	 */
	@Override
	public void afterConcurrentHandlingStarted(WebRequest request) {
		NDC.pop();
		if (NDC.getDepth() == 0) {
			NDC.remove();
		}
	}

}
