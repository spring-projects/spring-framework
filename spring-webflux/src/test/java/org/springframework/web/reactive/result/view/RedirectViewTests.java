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

package org.springframework.web.reactive.result.view;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for redirect view, and query string construction.
 * Doesn't test URL encoding, although it does check that it's called.
 *
 * @author Sebastien Deleuze
 */
public class RedirectViewTests {

	private MockServerWebExchange exchange;


	@BeforeEach
	public void setup() {
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/context/path").contextPath("/context"));
	}


	@Test
	public void noUrlSet() throws Exception {
		RedirectView rv = new RedirectView(null);
		assertThatIllegalArgumentException().isThrownBy(
				rv::afterPropertiesSet);
	}

	@Test
	public void defaultStatusCode() {
		String url = "https://url.somewhere.com";
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, this.exchange).block();
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create(url));
	}

	@Test
	public void customStatusCode() {
		String url = "https://url.somewhere.com";
		RedirectView view = new RedirectView(url, HttpStatus.FOUND);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, this.exchange).block();
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create(url));
	}

	@Test
	public void contextRelative() {
		String url = "/test.html";
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, this.exchange).block();
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/context/test.html"));
	}

	@Test
	public void contextRelativeQueryParam() {
		String url = "/test.html?id=1";
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, this.exchange).block();
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("/context/test.html?id=1"));
	}

	@Test
	public void remoteHost() {
		RedirectView view = new RedirectView("");

		assertThat(view.isRemoteHost("https://url.somewhere.com")).isFalse();
		assertThat(view.isRemoteHost("/path")).isFalse();
		assertThat(view.isRemoteHost("http://somewhereelse.example")).isFalse();

		view.setHosts("url.somewhere.com");

		assertThat(view.isRemoteHost("https://url.somewhere.com")).isFalse();
		assertThat(view.isRemoteHost("/path")).isFalse();
		assertThat(view.isRemoteHost("http://somewhereelse.example")).isTrue();
	}

	@Test
	public void expandUriTemplateVariablesFromModel() {
		String url = "https://url.somewhere.com?foo={foo}";
		Map<String, String> model = Collections.singletonMap("foo", "bar");
		RedirectView view = new RedirectView(url);
		view.render(model, MediaType.TEXT_HTML, this.exchange).block();
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("https://url.somewhere.com?foo=bar"));
	}

	@Test
	public void expandUriTemplateVariablesFromExchangeAttribute() {
		String url = "https://url.somewhere.com?foo={foo}";
		Map<String, String> attributes = Collections.singletonMap("foo", "bar");
		this.exchange.getAttributes().put(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, attributes);
		RedirectView view = new RedirectView(url);
		view.render(new HashMap<>(), MediaType.TEXT_HTML, exchange).block();
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("https://url.somewhere.com?foo=bar"));
	}

	@Test
	public void propagateQueryParams() throws Exception {
		RedirectView view = new RedirectView("https://url.somewhere.com?foo=bar#bazz");
		view.setPropagateQuery(true);
		this.exchange = MockServerWebExchange.from(MockServerHttpRequest.get("https://url.somewhere.com?a=b&c=d"));
		view.render(new HashMap<>(), MediaType.TEXT_HTML, this.exchange).block();
		assertThat(this.exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
		assertThat(this.exchange.getResponse().getHeaders().getLocation()).isEqualTo(URI.create("https://url.somewhere.com?foo=bar&a=b&c=d#bazz"));
	}

}
