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

package org.springframework.messaging.rsocket;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.Payload;
import io.rsocket.util.ByteBufPayload;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBuffer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PayloadUtils}.
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class PayloadUtilsTests {

	private LeakAwareNettyDataBufferFactory nettyBufferFactory =
			new LeakAwareNettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);


	@AfterEach
	public void tearDown() throws Exception {
		this.nettyBufferFactory.checkForLeaks(Duration.ofSeconds(5));
	}


	@Test
	public void retainAndReleaseWithNettyFactory() {
		Payload payload = ByteBufPayload.create("sample data");
		DataBuffer buffer = PayloadUtils.retainDataAndReleasePayload(payload, this.nettyBufferFactory);
		try {
			assertThat(buffer).isInstanceOf(NettyDataBuffer.class);
			assertThat(((NettyDataBuffer) buffer).getNativeBuffer().refCnt()).isEqualTo(1);
			assertThat(payload.refCnt()).isEqualTo(0);
		}
		finally {
			DataBufferUtils.release(buffer);
		}
	}

	@Test
	public void retainAndReleaseWithDefaultFactory() {
		Payload payload = ByteBufPayload.create("sample data");
		DataBuffer buffer = PayloadUtils.retainDataAndReleasePayload(payload, DefaultDataBufferFactory.sharedInstance);

		assertThat(buffer).isInstanceOf(DefaultDataBuffer.class);
		assertThat(payload.refCnt()).isEqualTo(0);
	}

	@Test
	public void createWithNettyBuffers() {
		NettyDataBuffer data = createNettyDataBuffer("sample data");
		NettyDataBuffer metadata = createNettyDataBuffer("sample metadata");

		Payload payload = PayloadUtils.createPayload(data, metadata);
		try {
			assertThat(payload).isInstanceOf(ByteBufPayload.class);
			assertThat(payload.data()).isSameAs(data.getNativeBuffer());
			assertThat(payload.metadata()).isSameAs(metadata.getNativeBuffer());
		}
		finally {
			payload.release();
		}
	}

	@Test
	public void createWithDefaultBuffers() {
		DataBuffer data = createDefaultDataBuffer("sample data");
		DataBuffer metadata = createDefaultDataBuffer("sample metadata");
		Payload payload = PayloadUtils.createPayload(data, metadata);

		assertThat(payload).isInstanceOf(DefaultPayload.class);
		assertThat(payload.getDataUtf8()).isEqualTo(data.toString(UTF_8));
		assertThat(payload.getMetadataUtf8()).isEqualTo(metadata.toString(UTF_8));
	}

	@Test
	public void createWithNettyAndDefaultBuffers() {
		NettyDataBuffer data = createNettyDataBuffer("sample data");
		DefaultDataBuffer metadata = createDefaultDataBuffer("sample metadata");
		Payload payload = PayloadUtils.createPayload(data, metadata);
		try {
			assertThat(payload).isInstanceOf(ByteBufPayload.class);
			assertThat(payload.data()).isSameAs(data.getNativeBuffer());
			assertThat(payload.getMetadataUtf8()).isEqualTo(metadata.toString(UTF_8));
		}
		finally {
			payload.release();
		}
	}

	@Test
	public void createWithDefaultAndNettyBuffers() {
		DefaultDataBuffer data = createDefaultDataBuffer("sample data");
		NettyDataBuffer metadata = createNettyDataBuffer("sample metadata");
		Payload payload = PayloadUtils.createPayload(data, metadata);
		try {
			assertThat(payload).isInstanceOf(ByteBufPayload.class);
			assertThat(payload.getDataUtf8()).isEqualTo(data.toString(UTF_8));
			assertThat(payload.metadata()).isSameAs(metadata.getNativeBuffer());
		}
		finally {
			payload.release();
		}
	}

	@Test
	public void createWithNettyBuffer() {
		NettyDataBuffer data = createNettyDataBuffer("sample data");
		Payload payload = PayloadUtils.createPayload(data);
		try {
			assertThat(payload).isInstanceOf(ByteBufPayload.class);
			assertThat(payload.data()).isSameAs(data.getNativeBuffer());
		}
		finally {
			payload.release();
		}
	}

	@Test
	public void createWithDefaultBuffer() {
		DataBuffer data = createDefaultDataBuffer("sample data");
		Payload payload = PayloadUtils.createPayload(data);

		assertThat(payload).isInstanceOf(DefaultPayload.class);
		assertThat(payload.getDataUtf8()).isEqualTo(data.toString(UTF_8));
	}


	private NettyDataBuffer createNettyDataBuffer(String content) {
		NettyDataBuffer buffer = this.nettyBufferFactory.allocateBuffer();
		buffer.write(content, StandardCharsets.UTF_8);
		return buffer;
	}

	private DefaultDataBuffer createDefaultDataBuffer(String content) {
		DefaultDataBuffer buffer = DefaultDataBufferFactory.sharedInstance.allocateBuffer();
		buffer.write(content, StandardCharsets.UTF_8);
		return buffer;
	}

}
