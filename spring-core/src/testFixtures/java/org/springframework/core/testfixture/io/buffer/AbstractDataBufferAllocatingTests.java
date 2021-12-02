/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.testfixture.io.buffer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PoolArenaMetric;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocatorMetric;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Base class for tests that read or write data buffers with an extension to check
 * that allocated buffers have been released.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public abstract class AbstractDataBufferAllocatingTests {

	@RegisterExtension
	AfterEachCallback leakDetector = context -> waitForDataBufferRelease(Duration.ofSeconds(2));

	protected DataBufferFactory bufferFactory;


	protected DataBuffer createDataBuffer(int capacity) {
		return this.bufferFactory.allocateBuffer(capacity);
	}

	protected DataBuffer stringBuffer(String value) {
		return byteBuffer(value.getBytes(StandardCharsets.UTF_8));
	}

	protected Mono<DataBuffer> deferStringBuffer(String value) {
		return Mono.defer(() -> Mono.just(stringBuffer(value)));
	}

	protected DataBuffer byteBuffer(byte[] value) {
		DataBuffer buffer = this.bufferFactory.allocateBuffer(value.length);
		buffer.write(value);
		return buffer;
	}

	protected void release(DataBuffer... buffers) {
		Arrays.stream(buffers).forEach(DataBufferUtils::release);
	}

	protected Consumer<DataBuffer> stringConsumer(String expected) {
		return stringConsumer(expected, UTF_8);
	}

	protected Consumer<DataBuffer> stringConsumer(String expected, Charset charset) {
		return dataBuffer -> {
			String value = dataBuffer.toString(charset);
			DataBufferUtils.release(dataBuffer);
			assertThat(value).isEqualTo(expected);
		};
	}

	/**
	 * Wait until allocations are at 0, or the given duration elapses.
	 */
	private void waitForDataBufferRelease(Duration duration) throws InterruptedException {
		Instant start = Instant.now();
		while (true) {
			try {
				verifyAllocations();
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

	private void verifyAllocations() {
		if (this.bufferFactory instanceof NettyDataBufferFactory) {
			ByteBufAllocator allocator = ((NettyDataBufferFactory) this.bufferFactory).getByteBufAllocator();
			if (allocator instanceof PooledByteBufAllocator) {
				Instant start = Instant.now();
				while (true) {
					PooledByteBufAllocatorMetric metric = ((PooledByteBufAllocator) allocator).metric();
					long total = getAllocations(metric.directArenas()) + getAllocations(metric.heapArenas());
					if (total == 0) {
						return;
					}
					if (Instant.now().isBefore(start.plus(Duration.ofSeconds(5)))) {
						try {
							Thread.sleep(50);
						}
						catch (InterruptedException ex) {
							// ignore
						}
						continue;
					}
					assertThat(total).as("ByteBuf Leak: " + total + " unreleased allocations").isEqualTo(0);
				}
			}
		}
	}

	private static long getAllocations(List<PoolArenaMetric> metrics) {
		return metrics.stream().mapToLong(PoolArenaMetric::numActiveAllocations).sum();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("org.springframework.core.testfixture.io.buffer.AbstractDataBufferAllocatingTests#dataBufferFactories()")
	public @interface ParameterizedDataBufferAllocatingTest {
	}

	@SuppressWarnings("deprecation") // PooledByteBufAllocator no longer supports tinyCacheSize.
	public static Stream<Arguments> dataBufferFactories() {
		return Stream.of(
			arguments("NettyDataBufferFactory - UnpooledByteBufAllocator - preferDirect = true",
					new NettyDataBufferFactory(new UnpooledByteBufAllocator(true))),
			arguments("NettyDataBufferFactory - UnpooledByteBufAllocator - preferDirect = false",
					new NettyDataBufferFactory(new UnpooledByteBufAllocator(false))),
			// 1) Disable caching for reliable leak detection, see https://github.com/netty/netty/issues/5275
			// 2) maxOrder is 4 (vs default 11) but can be increased if necessary
			arguments("NettyDataBufferFactory - PooledByteBufAllocator - preferDirect = true",
					new NettyDataBufferFactory(new PooledByteBufAllocator(true, 1, 1, 4096, 4, 0, 0, 0, true))),
			arguments("NettyDataBufferFactory - PooledByteBufAllocator - preferDirect = false",
					new NettyDataBufferFactory(new PooledByteBufAllocator(false, 1, 1, 4096, 4, 0, 0, 0, true))),
			arguments("DefaultDataBufferFactory - preferDirect = true",
					new DefaultDataBufferFactory(true)),
			arguments("DefaultDataBufferFactory - preferDirect = false",
					new DefaultDataBufferFactory(false))
		);
	}

}
