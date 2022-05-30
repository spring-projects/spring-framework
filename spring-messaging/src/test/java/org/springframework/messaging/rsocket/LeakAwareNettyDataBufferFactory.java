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

package org.springframework.messaging.rsocket;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.util.ObjectUtils;

/**
 * Unlike {@link org.springframework.core.testfixture.io.buffer.LeakAwareDataBufferFactory}
 * this one is an instance of {@link NettyDataBufferFactory} which is necessary
 * since {@link PayloadUtils} does instanceof checks, and that also allows
 * intercepting {@link NettyDataBufferFactory#wrap(ByteBuf)}.
 */
public class LeakAwareNettyDataBufferFactory extends NettyDataBufferFactory {

	private final List<DataBufferLeakInfo> created = new ArrayList<>();


	public LeakAwareNettyDataBufferFactory(ByteBufAllocator byteBufAllocator) {
		super(byteBufAllocator);
	}


	public void checkForLeaks(Duration duration) throws InterruptedException {
		Instant start = Instant.now();
		while (true) {
			try {
				this.created.forEach(info -> {
					if (((PooledDataBuffer) info.getDataBuffer()).isAllocated()) {
						throw info.getError();
					}
				});
				break;
			}
			catch (AssertionError ex) {
				if (Instant.now().isAfter(start.plus(duration))) {
					throw ex;
				}
			}
			Thread.sleep(50);
		}
	}

	public void reset() {
		this.created.clear();
	}


	@Override
	public NettyDataBuffer allocateBuffer() {
		return (NettyDataBuffer) recordHint(super.allocateBuffer());
	}

	@Override
	public NettyDataBuffer allocateBuffer(int initialCapacity) {
		return (NettyDataBuffer) recordHint(super.allocateBuffer(initialCapacity));
	}

	@Override
	public NettyDataBuffer wrap(ByteBuf byteBuf) {
		NettyDataBuffer dataBuffer = super.wrap(byteBuf);
		if (byteBuf != Unpooled.EMPTY_BUFFER) {
			recordHint(dataBuffer);
		}
		return dataBuffer;
	}

	@Override
	public DataBuffer join(List<? extends DataBuffer> dataBuffers) {
		return recordHint(super.join(dataBuffers));
	}

	private DataBuffer recordHint(DataBuffer buffer) {
		AssertionError error = new AssertionError(String.format(
				"DataBuffer leak: {%s} {%s} not released.%nStacktrace at buffer creation: ", buffer,
				ObjectUtils.getIdentityHexString(((NettyDataBuffer) buffer).getNativeBuffer())));
		this.created.add(new DataBufferLeakInfo(buffer, error));
		return buffer;
	}


	private static class DataBufferLeakInfo {

		private final DataBuffer dataBuffer;

		private final AssertionError error;

		DataBufferLeakInfo(DataBuffer dataBuffer, AssertionError error) {
			this.dataBuffer = dataBuffer;
			this.error = error;
		}

		DataBuffer getDataBuffer() {
			return this.dataBuffer;
		}

		AssertionError getError() {
			return this.error;
		}
	}
}
