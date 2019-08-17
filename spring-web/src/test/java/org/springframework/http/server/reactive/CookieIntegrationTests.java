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
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rossen Stoyanchev
 */
public class CookieIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final CookieHandler cookieHandler = new CookieHandler();


	@Override
	protected HttpHandler createHttpHandler() {
		return this.cookieHandler;
	}


	@ParameterizedHttpServerTest
	public void basicTest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		URI url = new URI("http://localhost:" + port);
		String header = "SID=31d4d96e407aad42; lang=en-US";
		ResponseEntity<Void> response = new RestTemplate().exchange(
				RequestEntity.get(url).header("Cookie", header).build(), Void.class);

		Map<String, List<HttpCookie>> requestCookies = this.cookieHandler.requestCookies;
		assertThat(requestCookies.size()).isEqualTo(2);

		List<HttpCookie> list = requestCookies.get("SID");
		assertThat(list.size()).isEqualTo(1);
		assertThat(list.iterator().next().getValue()).isEqualTo("31d4d96e407aad42");

		list = requestCookies.get("lang");
		assertThat(list.size()).isEqualTo(1);
		assertThat(list.iterator().next().getValue()).isEqualTo("en-US");

		List<String> headerValues = response.getHeaders().get("Set-Cookie");
		assertThat(headerValues.size()).isEqualTo(2);

		List<String> cookie0 = splitCookie(headerValues.get(0));
		assertThat(cookie0.remove("SID=31d4d96e407aad42")).as("SID").isTrue();
		assertThat(cookie0.stream().map(String::toLowerCase))
				.containsExactlyInAnyOrder("path=/", "secure", "httponly");
		List<String> cookie1 = splitCookie(headerValues.get(1));
		assertThat(cookie1.remove("lang=en-US")).as("lang").isTrue();
		assertThat(cookie1.stream().map(String::toLowerCase))
				.containsExactlyInAnyOrder("path=/", "domain=example.com");
	}

	// No client side HttpCookie support yet
	private List<String> splitCookie(String value) {
		List<String> list = new ArrayList<>();
		for (String s : value.split(";")){
			list.add(s.trim());
		}
		return list;
	}


	private class CookieHandler implements HttpHandler {

		private Map<String, List<HttpCookie>> requestCookies;


		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {

			this.requestCookies = request.getCookies();
			this.requestCookies.size(); // Cause lazy loading

			response.getCookies().add("SID", ResponseCookie.from("SID", "31d4d96e407aad42")
					.path("/").secure(true).httpOnly(true).build());
			response.getCookies().add("lang", ResponseCookie.from("lang", "en-US")
					.domain("example.com").path("/").build());

			return response.setComplete();
		}
	}

}
