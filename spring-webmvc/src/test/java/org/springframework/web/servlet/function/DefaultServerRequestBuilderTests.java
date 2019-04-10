/*
 * Copyright 2002-2019 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.mock.web.test.MockHttpServletRequest;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class DefaultServerRequestBuilderTests {

	private final List<HttpMessageConverter<?>> messageConverters = Collections.singletonList(
			new StringHttpMessageConverter());

	@Test
	public void from() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "https://example.com");
		request.addHeader("foo", "bar");

		ServerRequest other = ServerRequest.create(request, messageConverters);

		ServerRequest result = ServerRequest.from(other)
				.method(HttpMethod.HEAD)
				.header("foo", "bar")
				.headers(httpHeaders -> httpHeaders.set("baz", "qux"))
				.cookie("foo", "bar")
				.cookies(cookies -> cookies.set("baz", new Cookie("baz", "qux")))
				.attribute("foo", "bar")
				.attributes(attributes -> attributes.put("baz", "qux"))
				.body("baz")
				.build();

		assertEquals(HttpMethod.HEAD, result.method());
		assertEquals(2, result.headers().asHttpHeaders().size());
		assertEquals("bar", result.headers().asHttpHeaders().getFirst("foo"));
		assertEquals("qux", result.headers().asHttpHeaders().getFirst("baz"));
		assertEquals(2, result.cookies().size());
		assertEquals("bar", result.cookies().getFirst("foo").getValue());
		assertEquals("qux", result.cookies().getFirst("baz").getValue());
		assertEquals(2, result.attributes().size());
		assertEquals("bar", result.attributes().get("foo"));
		assertEquals("qux", result.attributes().get("baz"));

		String body = result.body(String.class);
		assertEquals("baz", body);
	}


}