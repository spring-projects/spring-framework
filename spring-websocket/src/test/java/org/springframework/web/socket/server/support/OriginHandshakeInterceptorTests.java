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
public class OriginHandshakeInterceptorTests extends AbstractHttpRequestTests {

	private final Map<String, Object> attributes = new HashMap<>();
	private final WebSocketHandler wsHandler = mock(WebSocketHandler.class);


	@Test
	public void invalidInput() {
		assertThatIllegalArgumentException().isThrownBy(() -> new OriginHandshakeInterceptor(null));
	}

	@Test
	public void originValueMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		List<String> allowed = Collections.singletonList("https://mydomain1.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	public void originValueNoMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		List<String> allowed = Collections.singletonList("https://mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

	@Test
	public void originListMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain2.example");
		List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	public void originListNoMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
		List<String> allowed = Arrays.asList("https://mydomain1.example", "https://mydomain2.example", "http://mydomain3.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(allowed);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

	@Test
	public void originNoMatchWithNullHostileCollection() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://www.mydomain4.example/");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
		Set<String> allowedOrigins = new ConcurrentSkipListSet<>();
		allowedOrigins.add("https://mydomain1.example");
		interceptor.setAllowedOrigins(allowedOrigins);
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

	@Test
	public void originMatchAll() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "https://mydomain1.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor();
		interceptor.setAllowedOrigins(Collections.singletonList("*"));
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	public void sameOriginMatchWithEmptyAllowedOrigins() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
		this.servletRequest.setServerName("mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	public void sameOriginMatchWithAllowedOrigins() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain2.example");
		this.servletRequest.setServerName("mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Arrays.asList("http://mydomain1.example"));
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isTrue();
		assertThat(HttpStatus.FORBIDDEN.value()).isNotEqualTo(servletResponse.getStatus());
	}

	@Test
	public void sameOriginNoMatch() throws Exception {
		this.servletRequest.addHeader(HttpHeaders.ORIGIN, "http://mydomain3.example");
		this.servletRequest.setServerName("mydomain2.example");
		OriginHandshakeInterceptor interceptor = new OriginHandshakeInterceptor(Collections.emptyList());
		assertThat(interceptor.beforeHandshake(request, response, wsHandler, attributes)).isFalse();
		assertThat(HttpStatus.FORBIDDEN.value()).isEqualTo(servletResponse.getStatus());
	}

}
