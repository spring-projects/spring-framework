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

package org.springframework.web.reactive.result;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.web.test.server.MockServerWebExchange;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.accept.FixedContentTypeResolver;
import org.springframework.web.reactive.accept.HeaderContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.ALL;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.IMAGE_GIF;
import static org.springframework.http.MediaType.IMAGE_JPEG;
import static org.springframework.http.MediaType.IMAGE_PNG;
import static org.springframework.http.MediaType.TEXT_PLAIN;

/**
 * Unit tests for {@link HandlerResultHandlerSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class HandlerResultHandlerTests {

	private final TestResultHandler resultHandler = new TestResultHandler();


	@Test
	public void usesContentTypeResolver() throws Exception {
		TestResultHandler resultHandler = new TestResultHandler(new FixedContentTypeResolver(IMAGE_GIF));
		List<MediaType> mediaTypes = Arrays.asList(IMAGE_JPEG, IMAGE_GIF, IMAGE_PNG);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
		MediaType actual = resultHandler.selectMediaType(exchange, () -> mediaTypes);

		assertThat(actual).isEqualTo(IMAGE_GIF);
	}

	@Test
	public void producibleMediaTypesRequestAttribute() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
		exchange.getAttributes().put(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, Collections.singleton(IMAGE_GIF));

		List<MediaType> mediaTypes = Arrays.asList(IMAGE_JPEG, IMAGE_GIF, IMAGE_PNG);
		MediaType actual = resultHandler.selectMediaType(exchange, () -> mediaTypes);

		assertThat(actual).isEqualTo(IMAGE_GIF);
	}

	@Test  // SPR-9160
	public void sortsByQuality() throws Exception {
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path")
				.header("Accept", "text/plain; q=0.5, application/json"));

		List<MediaType> mediaTypes = Arrays.asList(TEXT_PLAIN, APPLICATION_JSON);
		MediaType actual = this.resultHandler.selectMediaType(exchange, () -> mediaTypes);

		assertThat(actual).isEqualTo(APPLICATION_JSON);
	}

	@Test
	public void charsetFromAcceptHeader() throws Exception {
		MediaType text8859 = MediaType.parseMediaType("text/plain;charset=ISO-8859-1");
		MediaType textUtf8 = MediaType.parseMediaType("text/plain;charset=UTF-8");
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path").accept(text8859));
		MediaType actual = this.resultHandler.selectMediaType(exchange, () -> Collections.singletonList(textUtf8));

		assertThat(actual).isEqualTo(text8859);
	}

	@Test // SPR-12894
	public void noConcreteMediaType() throws Exception {
		List<MediaType> producible = Collections.singletonList(ALL);
		MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/path"));
		MediaType actual = this.resultHandler.selectMediaType(exchange, () -> producible);

		assertThat(actual).isEqualTo(APPLICATION_OCTET_STREAM);
	}


	@SuppressWarnings("WeakerAccess")
	private static class TestResultHandler extends HandlerResultHandlerSupport {

		protected TestResultHandler() {
			this(new HeaderContentTypeResolver());
		}

		public TestResultHandler(RequestedContentTypeResolver contentTypeResolver) {
			super(contentTypeResolver, ReactiveAdapterRegistry.getSharedInstance());
		}
	}

}
