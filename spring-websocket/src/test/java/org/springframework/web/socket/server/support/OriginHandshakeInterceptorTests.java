/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.server.support;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.WebSocketHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Test fixture for {@link OriginHandshakeInterceptor}.
 *
 * @author Sebastien Deleuze
 */
class OriginHandshakeInterceptorTests extends AbstractHttpRequestTests {

	private final Map<String, Object> attributes = new HashMap<>();
	private final WebSocketHandler wsHandler = mock();


	@Test
	void invalidInput() {
		assertThatIllegalArgumentException().isThrownBy(() -> new OriginHandshakeInterceptor(null));
	}

	@Test
	void originValueMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		List<String> allowed = Collections.singletonList("https://mydomain1.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	void originValueNoMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		List<String> allowed = Collections.singletonList("https://mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

	@Test
	void originListMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
		List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	void originListNoMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
		List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

	@Test
	void originNoMatchWithNullHostileCollection() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
		Set<String> allowedOrigins = new ConcurrentSkipListSet<>();
		allowedOrigins.add("https://mydomain1.example");
		interceptor.setAllowedOrigins(allowedOrigins);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

	@Test
	void originMatchAll() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
		interceptor.setAllowedOrigins(Collections.singletonList("*"));
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	void sameOriginMatchWithEmptyAllowedOrigins() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
		this.servletRequest.setServerName("mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	void sameOriginMatchWithAllowedOrigins() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
		this.servletRequest.setServerName("mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(List.of("http://mydomain1.example"));
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	void sameOriginNoMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain3.example");
		this.servletRequest.setServerName("mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

}
