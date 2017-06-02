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

package org.springframework.web.server.support;

import org.junit.Test;

import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link LookupPath}
 * @author Brian Clozel
 */
public class LookupPathTests {

	@Test
	public void parsePath() {
		LookupPath path = create("/foo");
		assertEquals("/foo", path.getPath());
		assertEquals("/foo", path.getPathWithoutExtension());
	}

	@Test
	public void parsePathWithExtension() {
		LookupPath path = create("/foo.txt");
		assertEquals("/foo.txt", path.getPath());
		assertEquals("/foo", path.getPathWithoutExtension());
		assertEquals(".txt", path.getFileExtension());
	}

	@Test
	public void parsePathWithParams() {
		LookupPath path = create("/test;spring=framework/foo.txt;foo=bar?framework=spring");
		assertEquals("/test;spring=framework/foo.txt;foo=bar", path.getPath());
		assertEquals("/test;spring=framework/foo", path.getPathWithoutExtension());
		assertEquals(".txt", path.getFileExtension());
	}

	private LookupPath create(String path) {
		HttpRequestPathHelper helper = new HttpRequestPathHelper();
		ServerWebExchange exchange = MockServerHttpRequest.get(path).build().toExchange();
		return helper.getLookupPathForRequest(exchange);
	}
}
