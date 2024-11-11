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

import java.util.Locale;
import java.util.stream.Stream;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.ReadOnlyHttpHeaders;
import io.undertow.util.HeaderMap;
import org.apache.tomcat.util.http.MimeHeaders;
import org.eclipse.jetty.http.HttpFields;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.BDDMockito;

import org.springframework.http.HttpHeaders;
import org.springframework.http.support.JettyHeadersAdapter;
import org.springframework.http.support.Netty4HeadersAdapter;
import org.springframework.http.support.Netty5HeadersAdapter;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.mockito.BDDMockito.when;

class DefaultServerHttpRequestBuilderTests {

	@ParameterizedTest
	@MethodSource("headers")
	void containerImmutableHeadersAreCopied(MultiValueMap<String, String> headerMap, boolean isMutableMap) {
		HttpHeaders originalHeaders = new HttpHeaders(headerMap);
		ServerHttpRequest mockRequest = createMockRequest(originalHeaders);
		final DefaultServerHttpRequestBuilder builder = new DefaultServerHttpRequestBuilder(mockRequest);

		//perform mutations on the map adapter of the container's headers if possible
		if (isMutableMap) {
			headerMap.set("CaseInsensitive", "original");
			assertThat(originalHeaders.getFirst("caseinsensitive"))
					.as("original mutated")
					.isEqualTo("original");
		}
		else {
			assertThatRuntimeException().isThrownBy(() -> headerMap.set("CaseInsensitive", "original"));
			assertThat(originalHeaders.getFirst("caseinsensitive"))
					.as("original not mutable")
					.isEqualTo("unmodified");
		}

		// Mutating the headers in the build. Note directly mutating via
		// .build().getHeaders() isn't applicable since/ headers are made
		// read-only by build()
		ServerHttpRequest req = builder
				.header("CaseInsensitive", "modified")
				.header("Additional", "header")
				.build();

		assertThat(req.getHeaders().getFirst("CaseInsensitive"))
				.as("copy mutated")
				.isEqualTo("modified");
		assertThat(req.getHeaders().getFirst("caseinsensitive"))
				.as("copy case-insensitive")
				.isEqualTo("modified");
		assertThat(req.getHeaders().getFirst("additional"))
				.as("copy has additional header")
				.isEqualTo("header");
	}

	private ServerHttpRequest createMockRequest(HttpHeaders originalHeaders) {
		//we can't use only use a MockServerHttpRequest because it uses a ReadOnlyHttpHeaders internally
		ServerHttpRequest mock = BDDMockito.spy(MockServerHttpRequest.get("/example").build());
		when(mock.getHeaders()).thenReturn(originalHeaders);

		return mock;
	}

	static Arguments initHeader(String description, MultiValueMap<String, String> headerMap) {
		headerMap.add("CaseInsensitive", "unmodified");
		return argumentSet(description, headerMap, true);
	}

	static Stream<Arguments> headers() {
		return Stream.of(
				initHeader("Map", CollectionUtils.toMultiValueMap(new LinkedCaseInsensitiveMap<>(8, Locale.ENGLISH))),
				initHeader("Netty", new Netty4HeadersAdapter(new DefaultHttpHeaders())),
				initHeader("Netty5", new Netty5HeadersAdapter(io.netty5.handler.codec.http.headers.HttpHeaders.newHeaders())),
				initHeader("Tomcat", new TomcatHeadersAdapter(new MimeHeaders())),
				initHeader("Undertow", new UndertowHeadersAdapter(new HeaderMap())),
				initHeader("Jetty", new JettyHeadersAdapter(HttpFields.build())),
				//immutable versions of some headers
				argumentSet("Netty immutable", new Netty4HeadersAdapter(new ReadOnlyHttpHeaders(false,
						"CaseInsensitive", "unmodified")), false),
				argumentSet("Jetty immutable", new JettyHeadersAdapter(HttpFields.build()
						.add("CaseInsensitive", "unmodified").asImmutable()), false)
		);
	}

}
