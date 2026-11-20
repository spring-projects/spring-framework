/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.result.view;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RequestContext}.
 *
 * @author Rossen Stoyanchev
 */
class RequestContextTests {

	private final MockServerWebExchange exchange =
			MockServerWebExchange.from(MockServerHttpRequest.get("/foo/path").contextPath("/foo"));

	private GenericApplicationContext applicationContext;

	private Map<String, Object> model = new HashMap<>();


	@BeforeEach
	void init() {
		this.applicationContext = new GenericApplicationContext();
		this.applicationContext.refresh();
	}

	@Test
	void getContextUrl() {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		assertThat(context.getContextUrl("bar")).isEqualTo("/foo/bar");
	}

	@Test
	void getContextUrlWithMap() {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("spam", "bucket");
		assertThat(context.getContextUrl("{foo}?spam={spam}", map)).isEqualTo("/foo/bar?spam=bucket");
	}

	@Test
	void getContextUrlWithMapEscaping() {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar baz");
		map.put("spam", "&bucket=");
		assertThat(context.getContextUrl("{foo}?spam={spam}", map)).isEqualTo("/foo/bar%20baz?spam=%26bucket%3D");
	}

	@Test
	void defaultHtmlEscapeNotConfigured() {
		RequestContext context = new RequestContext(this.exchange, this.model, this.applicationContext);
		assertThat(context.getDefaultHtmlEscape()).isNull();
		assertThat(context.isDefaultHtmlEscape()).isFalse();
	}
	@Test
	void defaultHtmlEscape() {
		MockServerWebExchange exchange = MockServerWebExchange.builder(
				MockServerHttpRequest.get("/foo/path").contextPath("/foo")).build();

		exchange.getAttributes().put(ServerWebExchange.HTML_ESCAPE_ATTRIBUTE, true);

		RequestContext context = new RequestContext(exchange, this.model, this.applicationContext);
		assertThat(context.getDefaultHtmlEscape()).isTrue();
		assertThat(context.isDefaultHtmlEscape()).isTrue();
	}

}
