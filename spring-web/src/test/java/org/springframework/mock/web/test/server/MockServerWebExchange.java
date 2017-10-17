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
package org.springframework.mock.web.test.server;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerHttpResponse;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.session.DefaultWebSessionManager;

/**
 * Variant of {@link DefaultServerWebExchange} for use in tests with
 * {@link MockServerHttpRequest} and {@link MockServerHttpResponse}.
 *
 * <p>See static factory methods to create an instance.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public final class MockServerWebExchange extends DefaultServerWebExchange {


	private MockServerWebExchange(MockServerHttpRequest request) {
		super(request, new MockServerHttpResponse(), new DefaultWebSessionManager(),
				ServerCodecConfigurer.create(), new AcceptHeaderLocaleContextResolver());
	}


	@Override
	public MockServerHttpResponse getResponse() {
		return (MockServerHttpResponse) super.getResponse();
	}


	/**
	 * Create a {@link MockServerWebExchange} from the given request.
	 * @param request the request to use.
	 * @return the exchange
	 */
	public static MockServerWebExchange from(MockServerHttpRequest request) {
		return new MockServerWebExchange(request);
	}

	/**
	 * A variant of {@link #from(MockServerHttpRequest)} that accepts a request
	 * builder. Internally invokes the {@code build()} to build the request.
	 * @param requestBuilder the builder for the request.
	 * @return the exchange
	 */
	public static MockServerWebExchange from(MockServerHttpRequest.BaseBuilder<?> requestBuilder) {
		return new MockServerWebExchange(requestBuilder.build());
	}

}
