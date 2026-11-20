/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.util.Random;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.AbstractHttpHandlerIntegrationTests;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class RandomHandlerIntegrationTests extends AbstractHttpHandlerIntegrationTests {

	private static final int REQUEST_SIZE = 4096 * 3;

	private static final int RESPONSE_SIZE = 1024 * 4;

	private final Random rnd = new Random();

	private final RandomHandler handler = new RandomHandler();


	@Override
	protected RandomHandler createHttpHandler() {
		return handler;
	}


	@ParameterizedHttpServerTest
	void random(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		// TODO: fix Reactor support

		ResponseEntity<byte[]> response = getRestClient().post().body(randomBytes()).retrieve().toEntity(byte[].class);

		assertThat(response.getBody()).isNotNull();
		assertThat(response.getHeaders().getContentLength()).isEqualTo(RESPONSE_SIZE);
		assertThat(response.getBody()).hasSize(RESPONSE_SIZE);
	}


	private byte[] randomBytes() {
		byte[] buffer = new byte[REQUEST_SIZE];
		rnd.nextBytes(buffer);
		return buffer;
	}

	private class RandomHandler implements HttpHandler {

		static final int CHUNKS = 16;

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			Mono<Integer> requestSizeMono = request.getBody().
					reduce(0, (integer, dataBuffer) -> integer +
							dataBuffer.readableByteCount()).
					doOnNext(size -> assertThat(size).isEqualTo(REQUEST_SIZE)).
					doOnError(throwable -> assertThat(throwable).isNull());

			response.getHeaders().setContentLength(RESPONSE_SIZE);

			return requestSizeMono.then(response.writeWith(multipleChunks()));
		}

		private Publisher<DataBuffer> multipleChunks() {
			int chunkSize = RESPONSE_SIZE / CHUNKS;
			return Flux.range(1, CHUNKS).map(integer -> randomBuffer(chunkSize));
		}

		private DataBuffer randomBuffer(int size) {
			byte[] bytes = new byte[size];
			rnd.nextBytes(bytes);
			DataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer(size);
			buffer.write(bytes);
			return buffer;
		}

	}

}
