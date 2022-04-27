/*
 * Copyright 2002-2020 the original author or authors.
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
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.servlet.handler.PathPatternsTestUtils;
import org.springframework.web.testfixture.servlet.MockCookie;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultServerRequestBuilderTests {

	private final List<HttpMessageConverter<?>> messageConverters =
			Collections.singletonList(new StringHttpMessageConverter());


	@Test
	void from() throws ServletException, IOException {
		MockHttpServletRequest request = PathPatternsTestUtils.initRequest("POST", "https://example.com", true);
		request.addHeader("foo", "bar");
		request.setCookies(new MockCookie("foo", "bar"));
		request.setAttribute("foo", "bar");
		request.addParameter("foo", "bar");
		request.setRemoteHost("127.0.0.1");
		request.setRemotePort(80);

		ServerRequest other = ServerRequest.create(request, messageConverters);

		ServerRequest result = ServerRequest.from(other)
				.method(HttpMethod.HEAD)
				.header("baz", "qux")
				.headers(httpHeaders -> httpHeaders.set("quux", "quuz"))
				.cookie("baz", "qux")
				.cookies(cookies -> cookies.set("quux", new Cookie("quux", "quuz")))
				.attribute("baz", "qux")
				.attributes(attributes -> attributes.put("quux", "quuz"))
				.param("baz", "qux")
				.params(params -> params.set("quux", "quuz"))
				.body("baz")
				.build();

		assertThat(result.method()).isEqualTo(HttpMethod.HEAD);
		assertThat(result.headers().asHttpHeaders().getFirst("foo")).isEqualTo("bar");
		assertThat(result.headers().asHttpHeaders().getFirst("baz")).isEqualTo("qux");
		assertThat(result.headers().asHttpHeaders().getFirst("quux")).isEqualTo("quuz");

		assertThat(result.cookies().getFirst("foo").getValue()).isEqualTo("bar");
		assertThat(result.cookies().getFirst("baz").getValue()).isEqualTo("qux");
		assertThat(result.cookies().getFirst("quux").getValue()).isEqualTo("quuz");

		assertThat(result.attributes().get("foo")).isEqualTo("bar");
		assertThat(result.attributes().get("baz")).isEqualTo("qux");
		assertThat(result.attributes().get("quux")).isEqualTo("quuz");

		assertThat(result.params().getFirst("foo")).isEqualTo("bar");
		assertThat(result.params().getFirst("baz")).isEqualTo("qux");
		assertThat(result.params().getFirst("quux")).isEqualTo("quuz");

		assertThat(result.remoteAddress()).contains(new InetSocketAddress("127.0.0.1", 80));

		String body = result.body(String.class);
		assertThat(body).isEqualTo("baz");
	}

}
