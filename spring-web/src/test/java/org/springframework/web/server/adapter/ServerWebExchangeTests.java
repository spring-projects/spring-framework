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

package org.springframework.web.server.adapter;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class ServerWebExchangeTests {

	private ServerWebExchange exchange;


	@Before
	public void createExchange() {
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("http://example.com"));
	}

	@Test
	public void transformUrlDefault() throws Exception {
		assertEquals("/foo", this.exchange.transformUrl("/foo"));
	}

	@Test
	public void transformUrlWithEncoder() throws Exception {
		this.exchange.addUrlTransformer(s -> s + "?nonce=123");
		assertEquals("/foo?nonce=123", this.exchange.transformUrl("/foo"));
	}

	@Test
	public void transformUrlWithMultipleEncoders() throws Exception {
		this.exchange.addUrlTransformer(s -> s + ";p=abc");
		this.exchange.addUrlTransformer(s -> s + "?q=123");
		assertEquals("/foo;p=abc?q=123", this.exchange.transformUrl("/foo"));
	}


}