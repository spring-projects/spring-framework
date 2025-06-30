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

package org.springframework.web.filter;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.util.Assert;
import org.springframework.web.util.WebUtils;

/**
 * A response wrapper used for the implementation of
 * {@link RelativeRedirectFilter} also shared with {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.10
 */
final class RelativeRedirectResponseWrapper extends HttpServletResponseWrapper {

	private final HttpStatusCode redirectStatus;


	private RelativeRedirectResponseWrapper(HttpServletResponse response, HttpStatusCode redirectStatus) {
		super(response);
		Assert.notNull(redirectStatus, "'redirectStatus' is required");
		this.redirectStatus = redirectStatus;
	}


	@Override
	public void sendRedirect(String location) throws IOException {
		resetBuffer();
		setStatus(this.redirectStatus.value());
		setHeader(HttpHeaders.LOCATION, location);
		flushBuffer();
	}


	public static HttpServletResponse wrapIfNecessary(HttpServletResponse response,
			HttpStatusCode redirectStatus) {

		RelativeRedirectResponseWrapper wrapper =
				WebUtils.getNativeResponse(response, RelativeRedirectResponseWrapper.class);

		return (wrapper != null ? response :
				new RelativeRedirectResponseWrapper(response, redirectStatus));
	}

}
