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

package org.springframework.http.server.reactive;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.UndertowHttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class CookieIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final CookieHandler cookieHandler = new CookieHandler();


	@Override
	protected HttpHandler createHttpHandler() {
		return this.cookieHandler;
	}


	@ParameterizedHttpServerTest
	public void basicTest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		URI url = URI.create("http://localhost:" + port);
		String header = "SID=31d4d96e407aad42; lang=en-US";
		ResponseEntity<Void> response = new RestTemplate().exchange(
				RequestEntity.get(url).header("Cookie", header).build(), Void.class);

		Map<String, List<HttpCookie>> requestCookies = this.cookieHandler.requestCookies;
		assertThat(requestCookies).hasSize(2);
		assertThat(requestCookies.get("SID")).extracting(HttpCookie::getValue).containsExactly("31d4d96e407aad42");
		assertThat(requestCookies.get("lang")).extracting(HttpCookie::getValue).containsExactly("en-US");

		List<String> headerValues = response.getHeaders().get("Set-Cookie");
		assertThat(headerValues).hasSize(2);

		List<String> cookie0 = splitCookie(headerValues.get(0));
		assertThat(cookie0.remove("SID=31d4d96e407aad42")).as("SID").isTrue();
		assertThat(cookie0.stream().map(String::toLowerCase))
				.contains("path=/", "secure", "httponly");
		List<String> cookie1 = splitCookie(headerValues.get(1));
		assertThat(cookie1.remove("lang=en-US")).as("lang").isTrue();
		assertThat(cookie1.stream().map(String::toLowerCase))
				.containsExactlyInAnyOrder("path=/", "domain=example.com");
	}

	@ParameterizedHttpServerTest
	public void partitionedAttributeTest(HttpServer httpServer) throws Exception {
		assumeFalse(httpServer instanceof UndertowHttpServer, "Undertow does not support Partitioned cookies");
		startServer(httpServer);

		URI url = URI.create("http://localhost:" + port);
		String header = "SID=31d4d96e407aad42; lang=en-US";
		ResponseEntity<Void> response = new RestTemplate().exchange(
				RequestEntity.get(url).header("Cookie", header).build(), Void.class);

		List<String> headerValues = response.getHeaders().get("Set-Cookie");
		assertThat(headerValues).hasSize(2);

		List<String> cookie0 = splitCookie(headerValues.get(0));
		assertThat(cookie0.remove("SID=31d4d96e407aad42")).as("SID").isTrue();
		assertThat(cookie0.stream().map(String::toLowerCase))
				.contains("partitioned");
	}

	@ParameterizedHttpServerTest
	public void cookiesWithSameNameTest(HttpServer httpServer) throws Exception {
		assumeFalse(httpServer instanceof UndertowHttpServer, "Bug in Undertow in Cookies with same name handling");

		startServer(httpServer);

		URI url = new URI("http://localhost:" + port);
		String header = "SID=31d4d96e407aad42; lang=en-US; lang=zh-CN";
		new RestTemplate().exchange(
				RequestEntity.get(url).header("Cookie", header).build(), Void.class);

		Map<String, List<HttpCookie>> requestCookies = this.cookieHandler.requestCookies;
		assertThat(requestCookies).hasSize(2);
		assertThat(requestCookies.get("SID")).extracting(HttpCookie::getValue).containsExactly("31d4d96e407aad42");
		assertThat(requestCookies.get("lang")).extracting(HttpCookie::getValue).containsExactly("en-US", "zh-CN");
	}

	// No client side HttpCookie support yet
	private List<String> splitCookie(String value) {
		List<String> list = new ArrayList<>();
		for (String s : value.split(";")){
			list.add(s.trim());
		}
		return list;
	}


	private static class CookieHandler implements HttpHandler {

		private Map<String, List<HttpCookie>> requestCookies;


		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {

			this.requestCookies = request.getCookies();
			this.requestCookies.size(); // Cause lazy loading

			response.getCookies().add("SID", ResponseCookie.from("SID", "31d4d96e407aad42")
					.path("/").secure(true).httpOnly(true).partitioned(true).build());
			response.getCookies().add("lang", ResponseCookie.from("lang", "en-US")
					.domain("example.com").path("/").build());

			return response.setComplete();
		}
	}

}
