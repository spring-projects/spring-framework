/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.List;

import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.server.NotAcceptableStatusException;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link HeaderContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderContentTypeResolverTests {

	private final HeaderContentTypeResolver resolver = new HeaderContentTypeResolver();


	@Test
	public void resolveMediaTypes() throws Exception {
		String header = "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c";
		List<MediaType> mediaTypes = this.resolver.resolveMediaTypes(
				MockServerWebExchange.from(MockServerHttpRequest.get("/").header("accept", header)));

		assertEquals(4, mediaTypes.size());
		assertEquals("text/html", mediaTypes.get(0).toString());
		assertEquals("text/x-c", mediaTypes.get(1).toString());
		assertEquals("text/x-dvi;q=0.8", mediaTypes.get(2).toString());
		assertEquals("text/plain;q=0.5", mediaTypes.get(3).toString());
	}

	@Test(expected = NotAcceptableStatusException.class)
	public void resolveMediaTypesParseError() throws Exception {
		String header = "textplain; q=0.5";
		this.resolver.resolveMediaTypes(
				MockServerWebExchange.from(MockServerHttpRequest.get("/").header("accept", header)));
	}

}
