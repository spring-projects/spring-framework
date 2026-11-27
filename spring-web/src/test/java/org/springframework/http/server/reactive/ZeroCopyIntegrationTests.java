/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.io.File;

import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.JettyCoreHttpServer;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.ReactorHttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author Arjen Poutsma
 */
class ZeroCopyIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final Resource springLogoResource = new ClassPathResource("/org/springframework/web/spring.png");

	private final ZeroCopyHandler handler = new ZeroCopyHandler();


	@Override
	protected HttpHandler createHttpHandler() {
		return this.handler;
	}


	@ParameterizedHttpServerTest
	void zeroCopy(HttpServer httpServer) throws Exception {
		assumeThat(httpServer).as("Zero-copy does not support Servlet")
				.isInstanceOfAny(ReactorHttpServer.class, JettyCoreHttpServer.class);

		startServer(httpServer);

		ResponseEntity<byte[]> response = getRestClient().get().retrieve().toEntity(byte[].class);

		assertThat(response.hasBody()).isTrue();
		assertThat(response.getHeaders().getContentLength()).isEqualTo(springLogoResource.contentLength());
		assertThat(response.getBody()).hasSize((int) springLogoResource.contentLength());
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
	}


	private static class ZeroCopyHandler implements HttpHandler {

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			try {
				ZeroCopyHttpOutputMessage zeroCopyResponse = (ZeroCopyHttpOutputMessage) response;
				File logoFile = springLogoResource.getFile();
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
