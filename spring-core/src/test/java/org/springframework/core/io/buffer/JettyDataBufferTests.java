/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.buffer;

import java.nio.ByteBuffer;

import org.eclipse.jetty.io.ArrayByteBufferPool;
import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.io.RetainableByteBuffer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link JettyDataBuffer}
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
class JettyDataBufferTests {

	private final JettyDataBufferFactory dataBufferFactory = new JettyDataBufferFactory();

	private ArrayByteBufferPool.Tracking byteBufferPool = new ArrayByteBufferPool.Tracking();

	@Test
	void releaseRetainChunk() {
		RetainableByteBuffer retainableBuffer = byteBufferPool.acquire(3, false);
		ByteBuffer buffer = retainableBuffer.getByteBuffer();
		buffer.position(0).limit(1);
		Content.Chunk chunk = Content.Chunk.asChunk(buffer, false, retainableBuffer);

		JettyDataBuffer dataBuffer = this.dataBufferFactory.wrap(chunk);
		dataBuffer.retain();
		dataBuffer.retain();
		assertThat(dataBuffer.release()).isFalse();
		assertThat(dataBuffer.release()).isFalse();
		assertThat(dataBuffer.release()).isTrue();

		assertThatIllegalStateException().isThrownBy(dataBuffer::release);
		assertThat(retainableBuffer.isRetained()).isFalse();
		assertThat(byteBufferPool.getLeaks()).isEmpty();
	}

	@AfterEach
	void tearDown() throws Exception {
		this.byteBufferPool.clear();
	}
}
