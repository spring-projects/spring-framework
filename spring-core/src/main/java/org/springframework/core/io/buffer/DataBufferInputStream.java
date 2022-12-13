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
import java.io.InputStream;

import org.springframework.util.Assert;

/**
 * An {@link InputStream} that reads from a {@link DataBuffer}.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see DataBuffer#asInputStream(boolean)
 */
final class DataBufferInputStream extends InputStream {

	private final DataBuffer dataBuffer;

	private final int end;

	private final boolean releaseOnClose;

	private boolean closed;

	private int mark;


	public DataBufferInputStream(DataBuffer dataBuffer, boolean releaseOnClose) {
		Assert.notNull(dataBuffer, "DataBuffer must not be null");
		this.dataBuffer = dataBuffer;
		int start = this.dataBuffer.readPosition();
		this.end = start + this.dataBuffer.readableByteCount();
		this.mark = start;
		this.releaseOnClose = releaseOnClose;
	}

	@Override
	public int read() throws IOException {
		checkClosed();
		if (available() == 0) {
			return -1;
		}
		return this.dataBuffer.read() & 0xFF;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		checkClosed();
		int available = available();
		if (available == 0) {
			return -1;
		}
		len = Math.min(available, len);
		this.dataBuffer.read(b, off, len);
		return len;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void mark(int readLimit) {
		Assert.isTrue(readLimit > 0, "readLimit must be greater than 0");
		this.mark = this.dataBuffer.readPosition();
	}

	@Override
	public int available() {
		return Math.max(0, this.end - this.dataBuffer.readPosition());
	}

	@Override
	public void reset() {
		this.dataBuffer.readPosition(this.mark);
	}

	@Override
	public void close() {
		if (this.closed) {
			return;
		}
		if (this.releaseOnClose) {
			DataBufferUtils.release(this.dataBuffer);
		}
		this.closed = true;
	}

	private void checkClosed() throws IOException {
		if (this.closed) {
			throw new IOException("DataBufferInputStream is closed");
		}
	}

}
