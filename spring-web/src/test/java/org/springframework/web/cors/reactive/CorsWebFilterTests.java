package org.springframework.web.cors.reactive;


import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.server.WebFilterChain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.HttpHeaders.*;

/**
 * Unit tests for {@link CorsWebFilter}.
 * @author Sebastien Deleuze
 */
public class CorsWebFilterTests {

	private CorsWebFilter filter;

	private final CorsConfiguration config = new CorsConfiguration();

	@Before
	public void setup() throws Exception {
		config.setAllowedOrigins(Arrays.asList("http://domain1.com", "http://domain2.com"));
		config.setAllowedMethods(Arrays.asList("GET", "POST"));
		config.setAllowedHeaders(Arrays.asList("header1", "header2"));
		config.setExposedHeaders(Arrays.asList("header3", "header4"));
		config.setMaxAge(123L);
		config.setAllowCredentials(false);
		filter = new CorsWebFilter(r -> config);
	}

	@Test
	public void validActualRequest() {

		MockServerHttpRequest request = MockServerHttpRequest
				.get("http://domain1.com/test.html")
				.header(HOST, "domain1.com")
				.header(ORIGIN, "http://domain2.com")
				.header("header2", "foo")
				.build();
		MockServerWebExchange exchange = new MockServerWebExchange(request);

		WebFilterChain filterChain = (filterExchange) -> {
			try {
				assertEquals("http://domain2.com", filterExchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
				assertEquals("header3, header4", filterExchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_EXPOSE_HEADERS));
			} catch (AssertionError ex) {
				return Mono.error(ex);
			}
			return Mono.empty();

		};
		filter.filter(exchange, filterChain);
	}

	@Test
	public void invalidActualRequest() throws ServletException, IOException {

		MockServerHttpRequest request = MockServerHttpRequest
				.delete("http://domain1.com/test.html")
				.header(HOST, "domain1.com")
				.header(ORIGIN, "http://domain2.com")
				.header("header2", "foo")
				.build();
		MockServerWebExchange exchange = new MockServerWebExchange(request);

		WebFilterChain filterChain = (filterExchange) -> Mono.error(new AssertionError("Invalid requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain);

		assertNull(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	public void validPreFlightRequest() throws ServletException, IOException {

		MockServerHttpRequest request = MockServerHttpRequest
				.options("http://domain1.com/test.html")
				.header(HOST, "domain1.com")
				.header(ORIGIN, "http://domain2.com")
				.header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2")
				.build();
		MockServerWebExchange exchange = new MockServerWebExchange(request);

		WebFilterChain filterChain = (filterExchange) -> Mono.error(new AssertionError("Preflight requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain);

		assertEquals("http://domain2.com", exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals("header1, header2", exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_HEADERS));
		assertEquals("header3, header4", exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_EXPOSE_HEADERS));
		assertEquals(123L, Long.parseLong(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_MAX_AGE)));
	}

	@Test
	public void invalidPreFlightRequest() throws ServletException, IOException {

		MockServerHttpRequest request = MockServerHttpRequest
				.options("http://domain1.com/test.html")
				.header(HOST, "domain1.com")
				.header(ORIGIN, "http://domain2.com")
				.header(ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.DELETE.name())
				.header(ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2")
				.build();
		MockServerWebExchange exchange = new MockServerWebExchange(request);

		WebFilterChain filterChain = (filterExchange) -> Mono.error(new AssertionError("Preflight requests must not be forwarded to the filter chain"));
		filter.filter(exchange, filterChain);

		assertNull(exchange.getResponse().getHeaders().getFirst(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

}
