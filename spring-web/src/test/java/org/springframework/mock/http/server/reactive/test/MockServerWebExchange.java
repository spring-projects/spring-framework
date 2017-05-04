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
package org.springframework.mock.http.server.reactive.test;

import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.server.ServerWebExchangeDecorator;
import org.springframework.web.server.adapter.DefaultServerWebExchange;
import org.springframework.web.server.session.DefaultWebSessionManager;

/**
 * {@code ServerWebExchange} for use in tests.
 *
 * <p>Effectively a wrapper around {@link DefaultServerWebExchange} plugged in
 * with {@link MockServerHttpRequest} and {@link MockServerHttpResponse}.
 *
 * <p>Typically used via {@link MockServerHttpRequest#toExchange()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class MockServerWebExchange extends ServerWebExchangeDecorator {


	public MockServerWebExchange(MockServerHttpRequest request) {
		super(new DefaultServerWebExchange(
				request, new MockServerHttpResponse(), new DefaultWebSessionManager(), ServerCodecConfigurer.create()));
	}


	@Override
	public MockServerHttpResponse getResponse() {
		return (MockServerHttpResponse) super.getResponse();
	}

}
