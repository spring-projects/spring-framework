/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.reactive;

import java.nio.charset.StandardCharsets;

import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpHeadResponseDecorator}.
 *
 * @author Rossen Stoyanchev
 */
class HttpHeadResponseDecoratorTests {

	private final LeakAwareDataBufferFactory bufferFactory =
			new LeakAwareDataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));

	private final ServerHttpResponse response =
			new HttpHeadResponseDecorator(new MockServerHttpResponse(this.bufferFactory));


	@AfterEach
	void tearDown() {
		this.bufferFactory.checkForLeaks();
	}


	@Test
	void writeWithFlux() {
		Flux<DataBuffer> body = Flux.just(toDataBuffer("data1"), toDataBuffer("data2"));
		this.response.writeWith(body).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
	}

	@Test
	void writeWithMono() {
		Mono<DataBuffer> body = Mono.just(toDataBuffer("data1,data2"));
		this.response.writeWith(body).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(11);
	}

	@Test // gh-23484
	void writeWithGivenContentLength() {
		int length = 15;
		this.response.getHeaders().setContentLength(length);
		this.response.writeWith(Flux.empty()).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(length);
	}

	@Test // gh-25908
	void writeWithGivenTransferEncoding() {
		Flux<DataBuffer> body = Flux.just(toDataBuffer("data1"), toDataBuffer("data2"));
		this.response.getHeaders().add(HttpHeaders.TRANSFER_ENCODING, "chunked");
		this.response.writeWith(body).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
	}

	private DataBuffer toDataBuffer(String s) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(256);
		buffer.write(s.getBytes(StandardCharsets.UTF_8));
		return buffer;
	}

}
