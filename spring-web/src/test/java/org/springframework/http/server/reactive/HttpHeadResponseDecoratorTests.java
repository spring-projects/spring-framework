/*
 * Copyright 2002-2020 the original author or authors.
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
 * Unit tests for {@link HttpHeadResponseDecorator}.
 * @author Rossen Stoyanchev
 */
public class HttpHeadResponseDecoratorTests {

	private final LeakAwareDataBufferFactory bufferFactory =
			new LeakAwareDataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT));

	private final ServerHttpResponse response =
			new HttpHeadResponseDecorator(new MockServerHttpResponse(this.bufferFactory));


	@AfterEach
	public void tearDown() {
		this.bufferFactory.checkForLeaks();
	}


	@Test
	public void writeWithFlux() {
		Flux<DataBuffer> body = Flux.just(toDataBuffer("data1"), toDataBuffer("data2"));
		this.response.writeWith(body).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
	}

	@Test
	public void writeWithMono() {
		Mono<DataBuffer> body = Mono.just(toDataBuffer("data1,data2"));
		this.response.writeWith(body).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(11);
	}

	@Test // gh-23484
	public void writeWithGivenContentLength() {
		int length = 15;
		this.response.getHeaders().setContentLength(length);
		this.response.writeWith(Flux.empty()).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(length);
	}

	@Test // gh-25908
	public void writeWithGivenTransferEncoding() {
		Flux<DataBuffer> body = Flux.just(toDataBuffer("data1"), toDataBuffer("data2"));
		this.response.getHeaders().add(HttpHeaders.TRANSFER_ENCODING, "chunked");
		this.response.writeWith(body).block();
		assertThat(this.response.getHeaders().getContentLength()).isEqualTo(-1);
	}

	private DataBuffer toDataBuffer(String s) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer();
		buffer.write(s.getBytes(StandardCharsets.UTF_8));
		return buffer;
	}

}
