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

package org.springframework.web.context.request.async;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;

/**
 * An implementation of {@link AsyncWebRequest} used when no underlying support
 * for async request processing is available in which case {@link #startAsync()}
 * results in an {@link UnsupportedOperationException}.
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class NoOpAsyncWebRequest extends ServletWebRequest implements AsyncWebRequest {

	public NoOpAsyncWebRequest(HttpServletRequest request, HttpServletResponse response) {
		super(request, response);
	}

	public void setTimeout(Long timeout) {
	}

	public void setTimeoutHandler(Runnable runnable) {
	}

	public boolean isAsyncStarted() {
		return false;
	}

	public boolean isAsyncCompleted() {
		return false;
	}

	public void startAsync() {
		throw new UnsupportedOperationException("No async support in a pre-Servlet 3.0 runtime");
	}

	public void complete() {
		throw new UnsupportedOperationException("No async support in a pre-Servlet 3.0 environment");
	}

	public void sendError(HttpStatus status, String message) {
		throw new UnsupportedOperationException("No async support in a pre-Servlet 3.0 environment");
	}

}
