/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.reactive.function;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.MockServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Arjen Poutsma
 */
public class ResourceResponseTests {

	private final Resource resource = new ClassPathResource("response.txt", ResourceResponseTests.class);

	private final ResourceResponse resourceResponse =
			new ResourceResponse(200, new HttpHeaders(), resource);

	@Test
	public void body() throws Exception {
		assertEquals(resource, resourceResponse.body());
	}

	@Test
	public void writeTo() throws Exception {
		ServerWebExchange exchange = mock(ServerWebExchange.class);
		MockServerHttpResponse response = new MockServerHttpResponse();
		when(exchange.getResponse()).thenReturn(response);


		resourceResponse.writeTo(exchange).block();
		assertNotNull(response.getBody());
	}

}