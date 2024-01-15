/*
 * Copyright 2002-2024 the original author or authors.
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

import org.eclipse.jetty.io.Content;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * @author Arjen Poutsma
 */
public class JettyDataBufferTests {

	private final JettyDataBufferFactory dataBufferFactory = new JettyDataBufferFactory();

	@Test
	void releaseRetainChunk() {
		ByteBuffer buffer = ByteBuffer.allocate(3);
		Content.Chunk mockChunk = mock();
		given(mockChunk.getByteBuffer()).willReturn(buffer);
		given(mockChunk.release()).willReturn(false, false, true);



		JettyDataBuffer dataBuffer = this.dataBufferFactory.wrap(mockChunk);
		dataBuffer.retain();
		dataBuffer.retain();
		assertThat(dataBuffer.release()).isFalse();
		assertThat(dataBuffer.release()).isFalse();
		assertThat(dataBuffer.release()).isTrue();

		assertThatIllegalStateException().isThrownBy(dataBuffer::release);

		then(mockChunk).should(times(3)).retain();
		then(mockChunk).should(times(3)).release();
	}
}
