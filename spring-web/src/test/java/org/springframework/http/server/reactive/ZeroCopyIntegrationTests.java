/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.server.reactive;

import java.io.File;
import java.net.URI;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.server.reactive.bootstrap.ReactorHttpServer;
import org.springframework.http.server.reactive.bootstrap.UndertowHttpServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * @author Arjen Poutsma
 */
public class ZeroCopyIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private final ZeroCopyHandler handler = new ZeroCopyHandler();


	@Override
	protected HttpHandler createHttpHandler() {
		return this.handler;
	}


	@Test
	public void zeroCopy() throws Exception {
		// Zero-copy only does not support servlet
		assumeTrue(server instanceof ReactorHttpServer || server instanceof UndertowHttpServer);

		URI url = new URI("http://localhost:" + port);
		RequestEntity<?> request = RequestEntity.get(url).build();
		ResponseEntity<byte[]> response = new RestTemplate().exchange(request, byte[].class);

		Resource logo = new ClassPathResource("spring.png", ZeroCopyIntegrationTests.class);

		assertTrue(response.hasBody());
		assertEquals(logo.contentLength(), response.getHeaders().getContentLength());
		assertEquals(logo.contentLength(), response.getBody().length);
		assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
	}


	private static class ZeroCopyHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			try {
				ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
				Resource logo = new ClassPathResource("spring.png", ZeroCopyIntegrationTests.class);
				File logoFile = logo.getFile();
				zeroCopyResponse.getHeaders().setContentType(MediaType.IMAGE_PNG);
				zeroCopyResponse.getHeaders().setContentLength(logoFile.length());
				return zeroCopyResponse.writeWith(logoFile, 0, logoFile.length());
			}
			catch (Throwable ex) {
				return Mono.error(ex);
			}
		}
	}

}
