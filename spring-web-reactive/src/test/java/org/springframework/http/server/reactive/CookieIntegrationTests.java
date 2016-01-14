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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import reactor.Mono;

import org.springframework.http.HttpCookie;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.boot.HttpServer;
import org.springframework.http.server.reactive.boot.JettyHttpServer;
import org.springframework.http.server.reactive.boot.ReactorHttpServer;
import org.springframework.http.server.reactive.boot.RxNettyHttpServer;
import org.springframework.http.server.reactive.boot.TomcatHttpServer;
import org.springframework.http.server.reactive.boot.UndertowHttpServer;
import org.springframework.util.SocketUtils;
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
public class CookieIntegrationTests {

	protected int port;

	@Parameterized.Parameter(0)
	public HttpServer server;

	private CookieHandler cookieHandler;


	@Parameterized.Parameters(name = "server [{0}]")
	public static Object[][] arguments() {
		return new Object[][] {
				{new JettyHttpServer()},
				{new RxNettyHttpServer()},
				{new ReactorHttpServer()},
				{new TomcatHttpServer()},
				{new UndertowHttpServer()}
		};
	}


	@Before
	public void setup() throws Exception {
		this.port = SocketUtils.findAvailableTcpPort();
		this.server.setPort(this.port);
		this.server.setHandler(createHttpHandler());
		this.server.afterPropertiesSet();
		this.server.start();
	}

	protected HttpHandler createHttpHandler() {
		this.cookieHandler = new CookieHandler();
		return this.cookieHandler;
	}

	@After
	public void tearDown() throws Exception {
		this.server.stop();
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

			this.requestCookies = request.getHeaders().getCookies();
			this.requestCookies.size(); // Cause lazy loading

			response.getHeaders().addCookie(HttpCookie.serverCookie("SID", "31d4d96e407aad42")
					.path("/").secure().httpOnly().build());
			response.getHeaders().addCookie(HttpCookie.serverCookie("lang", "en-US")
					.domain("example.com").path("/").build());
			response.writeHeaders();

			return Mono.empty();
		}
	}

}
