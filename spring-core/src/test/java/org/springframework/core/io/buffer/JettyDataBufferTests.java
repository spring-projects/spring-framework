/*
 * Copyright 2002-present the original author or authors.
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
public class JettyDataBufferTests {

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
	public void tearDown() throws Exception {
		this.byteBufferPool.clear();
	}
}
