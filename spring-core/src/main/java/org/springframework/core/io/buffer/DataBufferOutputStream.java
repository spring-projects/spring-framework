/*
 * Copyright 2002-2022 the original author or authors.
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

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.util.Assert;

/**
 * An {@link OutputStream} that writes to a {@link DataBuffer}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see DataBuffer#asOutputStream()
 */
final class DataBufferOutputStream extends OutputStream {

	private final DataBuffer dataBuffer;

	private boolean closed;


	public DataBufferOutputStream(DataBuffer dataBuffer) {
		Assert.notNull(dataBuffer, "DataBuffer must not be null");
		this.dataBuffer = dataBuffer;
	}

	@Override
	public void write(int b) throws IOException {
		checkClosed();
		this.dataBuffer.ensureWritable(1);
		this.dataBuffer.write((byte) b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		checkClosed();
		if (len > 0) {
			this.dataBuffer.ensureWritable(len);
			this.dataBuffer.write(b, off, len);
		}
	}

	@Override
	public void close() {
		if (this.closed) {
			return;
		}
		this.closed = true;
	}

	private void checkClosed() throws IOException {
		if (this.closed) {
			throw new IOException("DataBufferOutputStream is closed");
		}
	}

}
