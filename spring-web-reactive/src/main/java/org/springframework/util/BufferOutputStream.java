/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.util;

import java.io.IOException;
import java.io.OutputStream;

import reactor.io.buffer.Buffer;

/**
 * Simple extension of {@link OutputStream} that uses {@link Buffer} to stream
 * the content
 *
 * @author Sebastien Deleuze
 */
public class BufferOutputStream extends OutputStream {

	private Buffer buffer;

	public BufferOutputStream(Buffer buffer) {
		this.buffer = buffer;
	}

	@Override
	public void write(int b) throws IOException {
		buffer.append(b);
	}

	@Override
	public void write(byte[] bytes, int off, int len)
			throws IOException {
		buffer.append(bytes, off, len);
	}

	public Buffer getBuffer() {
		return buffer;
	}

}
