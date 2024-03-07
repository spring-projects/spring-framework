/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.servlet.ModelAndView;

/**
 * {@link AsyncServerResponse} implementation for completed futures.
 *
 * @author Arjen Poutsma
 * @since 6.2
 */
final class CompletedAsyncServerResponse implements AsyncServerResponse {

	private final ServerResponse serverResponse;


	CompletedAsyncServerResponse(ServerResponse serverResponse) {
		Assert.notNull(serverResponse, "ServerResponse must not be null");
		this.serverResponse = serverResponse;
	}

	@Override
	public ServerResponse block() {
		return this.serverResponse;
	}

	@Override
	public HttpStatusCode statusCode() {
		return this.serverResponse.statusCode();
	}

	@Override
	@Deprecated
	public int rawStatusCode() {
		return this.serverResponse.rawStatusCode();
	}

	@Override
	public HttpHeaders headers() {
		return this.serverResponse.headers();
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return this.serverResponse.cookies();
	}

	@Nullable
	@Override
	public ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException {

		return this.serverResponse.writeTo(request, response, context);
	}
}
