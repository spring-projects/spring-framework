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

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import reactor.netty.http.server.HttpServerRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link ReactorUriHelper}.
 *
 * @author Arjen Poutsma
 */
class ReactorUriHelperTests {

	@Test
	void hostnameWithZoneId() throws URISyntaxException {
		HttpServerRequest nettyRequest = mock();

		given(nettyRequest.scheme()).willReturn("http");
		given(nettyRequest.hostName()).willReturn("fe80::a%en1");
		given(nettyRequest.hostPort()).willReturn(80);
		given(nettyRequest.uri()).willReturn("/");

		URI uri = ReactorUriHelper.createUri(nettyRequest);
		assertThat(uri).hasScheme("http")
				.hasHost("[fe80::a%25en1]")
				.hasPort(-1)
				.hasPath("/")
				.hasToString("http://[fe80::a%25en1]/");

	}

	@ParameterizedTest(name = "{displayName}({arguments})")
	@CsvSource(delimiter='|', value = {
			"/prefix           | /prefix/",
			"/prefix1/prefix2  | /prefix1/prefix2/",
			"                  | /",
			"''                | /",
	})
	void forwardedPrefix(String forwardedPrefixHeader, String expectedPath) throws URISyntaxException {
		HttpServerRequest nettyRequest = mock();

		given(nettyRequest.scheme()).willReturn("https");
		given(nettyRequest.hostName()).willReturn("localhost");
		given(nettyRequest.hostPort()).willReturn(443);
		given(nettyRequest.uri()).willReturn("/");
		given(nettyRequest.forwardedPrefix()).willReturn(forwardedPrefixHeader);

		URI uri = ReactorUriHelper.createUri(nettyRequest);
		assertThat(uri).hasScheme("https")
				.hasHost("localhost")
				.hasPort(-1)
				.hasPath(expectedPath)
				.hasToString("https://localhost" + expectedPath);
	}

}
