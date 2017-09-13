/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.support;

import java.net.URI;

import org.junit.Test;

import org.springframework.web.reactive.function.server.MockServerRequest;
import org.springframework.web.util.UriComponents;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class WebFluxUriComponentsBuilderTests {

	@Test
	public void fromServerRequest() throws Exception {
		URI uri = new URI("http", "localhost", "/path", "a=1", null);
		MockServerRequest request = MockServerRequest.builder().uri(uri).build();

		UriComponents result = WebFluxUriComponentsBuilder.fromServerRequest(request).build();
		assertEquals("http", result.getScheme());
		assertEquals("localhost", result.getHost());
		assertEquals(-1, result.getPort());
		assertEquals("/path", result.getPath());
		assertEquals("a=1", result.getQuery());
	}

}