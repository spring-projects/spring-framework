/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.support.ServerRequestWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
class HeadersWrapperTests {

	private ServerRequest.Headers mockHeaders = mock();

	private ServerRequestWrapper.HeadersWrapper wrapper = new ServerRequestWrapper.HeadersWrapper(mockHeaders);


	@Test
	void accept() {
		List<MediaType> accept = Collections.singletonList(MediaType.APPLICATION_JSON);
		given(mockHeaders.accept()).willReturn(accept);

		assertThat(wrapper.accept()).isSameAs(accept);
	}

	@Test
	void acceptCharset() {
		List<Charset> acceptCharset = Collections.singletonList(StandardCharsets.UTF_8);
		given(mockHeaders.acceptCharset()).willReturn(acceptCharset);

		assertThat(wrapper.acceptCharset()).isSameAs(acceptCharset);
	}

	@Test
	void contentLength() {
		OptionalLong contentLength = OptionalLong.of(42L);
		given(mockHeaders.contentLength()).willReturn(contentLength);

		assertThat(wrapper.contentLength()).isSameAs(contentLength);
	}

	@Test
	void contentType() {
		Optional<MediaType> contentType = Optional.of(MediaType.APPLICATION_JSON);
		given(mockHeaders.contentType()).willReturn(contentType);

		assertThat(wrapper.contentType()).isSameAs(contentType);
	}

	@Test
	void host() {
		InetSocketAddress host = InetSocketAddress.createUnresolved("example.com", 42);
		given(mockHeaders.host()).willReturn(host);

		assertThat(wrapper.host()).isSameAs(host);
	}

	@Test
	void range() {
		List<HttpRange> range = Collections.singletonList(HttpRange.createByteRange(42));
		given(mockHeaders.range()).willReturn(range);

		assertThat(wrapper.range()).isSameAs(range);
	}

	@Test
	void header() {
		String name = "foo";
		List<String> value = Collections.singletonList("bar");
		given(mockHeaders.header(name)).willReturn(value);

		assertThat(wrapper.header(name)).isSameAs(value);
	}

	@Test
	void asHttpHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		given(mockHeaders.asHttpHeaders()).willReturn(httpHeaders);

		assertThat(wrapper.asHttpHeaders()).isSameAs(httpHeaders);
	}

}
