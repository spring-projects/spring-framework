/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
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
	public HttpHeaders headers() {
		return this.serverResponse.headers();
	}

	@Override
	public MultiValueMap<String, Cookie> cookies() {
		return this.serverResponse.cookies();
	}

	@Override
	public @Nullable ModelAndView writeTo(HttpServletRequest request, HttpServletResponse response, Context context)
			throws ServletException, IOException {

		return this.serverResponse.writeTo(request, response, context);
	}

}
