/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataBuffers}.
 */
class DataBuffersTests {

	@Test
	void compositeMatcherUsesBulkAccess() {
		DataBuffer delegate = new DefaultDataBufferFactory()
				.wrap("--boundaryXline one".getBytes(StandardCharsets.US_ASCII));
		AtomicInteger getByteCalls = new AtomicInteger();
		DataBuffer buffer = new DataBufferWrapper(delegate) {
			@Override
			public byte getByte(int index) {
				getByteCalls.incrementAndGet();
				return super.getByte(index);
			}
			@Override
			public int forEachByte(int index, int length, ByteProcessor processor) {
				return delegate.forEachByte(index, length, processor);
			}
		};

		DataBufferMatcher matcher = DataBuffers.matcher(
				"--boundary\r\n".getBytes(StandardCharsets.US_ASCII),
				"--boundary--".getBytes(StandardCharsets.US_ASCII));

		// Byte-by-byte access through DataBuffer.getByte is costly for some implementations
		// (e.g. Netty's leak-aware buffers record an access per call), so the matcher must
		// use the bulk DataBuffer.forEachByte path.
		assertThat(matcher.match(buffer)).isEqualTo(-1);
		assertThat(getByteCalls).hasValue(0);
	}

}
