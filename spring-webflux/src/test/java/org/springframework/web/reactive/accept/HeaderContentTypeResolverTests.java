/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link HeaderContentTypeResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class HeaderContentTypeResolverTests {

	private final HeaderContentTypeResolver resolver = new HeaderContentTypeResolver();


	@Test
	public void resolveMediaTypes() throws Exception {
		String header = "text/plain; q=0.5, text/html, text/x-dvi; q=0.8, text/x-c";
		List<MediaType> mediaTypes = this.resolver.resolveMediaTypes(
				MockServerWebExchange.from(MockServerHttpRequest.get("/").header("accept", header)));

		assertThat(mediaTypes).hasSize(4);
		assertThat(mediaTypes.get(0).toString()).isEqualTo("text/html");
		assertThat(mediaTypes.get(1).toString()).isEqualTo("text/x-c");
		assertThat(mediaTypes.get(2).toString()).isEqualTo("text/x-dvi;q=0.8");
		assertThat(mediaTypes.get(3).toString()).isEqualTo("text/plain;q=0.5");
	}

	@Test
	public void resolveMediaTypesParseError() throws Exception {
		String header = "textplain; q=0.5";
		assertThatExceptionOfType(NotAcceptableStatusException.class).isThrownBy(() ->
				this.resolver.resolveMediaTypes(
						MockServerWebExchange.from(MockServerHttpRequest.get("/").header("accept", header))));
	}

}
