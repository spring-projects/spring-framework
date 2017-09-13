/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.support;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Arjen Poutsma
 * @since 5.0
 */
public class WebFluxUriComponentsBuilder {

	/**
	 * Create a new {@code UriComponents} object from the URI associated with
	 * the given {@code ServerRequest} while also overlaying with values from the headers
	 * "Forwarded" (<a href="http://tools.ietf.org/html/rfc7239">RFC 7239</a>),
	 * or "X-Forwarded-Host", "X-Forwarded-Port", and "X-Forwarded-Proto" if
	 * "Forwarded" is not found.
	 * @param request the source request
	 * @return the URI components of the URI
	 */
	public static UriComponentsBuilder fromServerRequest(ServerRequest request) {
		Assert.notNull(request, "'request' must not be null");

		return UriComponentsBuilder.fromHttpRequest(new ServerRequestAdapter(request));
	}


	private static final class ServerRequestAdapter implements HttpRequest {

		private final ServerRequest adaptee;

		public ServerRequestAdapter(ServerRequest adaptee) {
			this.adaptee = adaptee;
		}

		@Override
		public String getMethodValue() {
			return this.adaptee.methodName();
		}

		@Override
		public URI getURI() {
			return this.adaptee.uri();
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.adaptee.headers().asHttpHeaders();
		}
	}

}
