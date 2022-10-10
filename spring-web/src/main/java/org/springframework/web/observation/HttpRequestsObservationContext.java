/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.observation;

import io.micrometer.observation.transport.RequestReplyReceiverContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.Nullable;

/**
 * Context that holds information for metadata collection during observations for Servlet web application.
 * <p>This context also extends {@link RequestReplyReceiverContext} for propagating
 * tracing information with the HTTP server exchange.
 * @author Brian Clozel
 * @since 6.0
 */
public class HttpRequestsObservationContext extends RequestReplyReceiverContext<HttpServletRequest, HttpServletResponse> {

	@Nullable
	private String pathPattern;

	public HttpRequestsObservationContext(HttpServletRequest request, HttpServletResponse response) {
		super(HttpServletRequest::getHeader);
		setCarrier(request);
		setResponse(response);
	}

	@Nullable
	public String getPathPattern() {
		return this.pathPattern;
	}

	public void setPathPattern(@Nullable String pathPattern) {
		this.pathPattern = pathPattern;
	}
}
