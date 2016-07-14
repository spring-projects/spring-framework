/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.http.server.reactive;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class CookieIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private CookieHandler cookieHandler;

	@Override
	protected HttpHandler createHttpHandler() {
		this.cookieHandler = new CookieHandler();
		return this.cookieHandler;
	}


	@SuppressWarnings("unchecked")
	@Test
	public void basicTest() throws Exception {
		URI url = new URI("http://localhost:" + port);
		String header = "SID=31d4d96e407aad42; lang=en-US";
		ResponseEntity<Void> response = new RestTemplate().exchange(
				RequestEntity.get(url).header("Cookie", header).build(), Void.class);

		Map<String, List<HttpCookie>> requestCookies = this.cookieHandler.requestCookies;
		assertEquals(2, requestCookies.size());

		List<HttpCookie> list = requestCookies.get("SID");
		assertEquals(1, list.size());
		assertEquals("31d4d96e407aad42", list.iterator().next().getValue());

		list = requestCookies.get("lang");
		assertEquals(1, list.size());
		assertEquals("en-US", list.iterator().next().getValue());

		List<String> headerValues = response.getHeaders().get("Set-Cookie");
		assertEquals(2, headerValues.size());

		assertThat(splitCookie(headerValues.get(0)), containsInAnyOrder(equalTo("SID=31d4d96e407aad42"),
				equalToIgnoringCase("Path=/"), equalToIgnoringCase("Secure"), equalToIgnoringCase("HttpOnly")));

		assertThat(splitCookie(headerValues.get(1)), containsInAnyOrder(equalTo("lang=en-US"),
				equalToIgnoringCase("Path=/"), equalToIgnoringCase("Domain=example.com")));
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
