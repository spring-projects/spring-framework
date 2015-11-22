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
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Simple {@link InputStream} implementation that exposes currently
 * available content of a {@link ByteBuffer}.
 *
 * From Jackson <a href="https://github.com/FasterXML/jackson-databind/blob/master/src/main/java/com/fasterxml/jackson/databind/util/ByteBufferBackedInputStream.java">ByteBufferBackedInputStream</a>
 */
public class ByteBufferInputStream extends InputStream {

	protected final ByteBuffer b;

	public ByteBufferInputStream(ByteBuffer buf) {
		b = buf;
	}

	@Override
	public int available() {
		return b.remaining();
	}

	@Override
	public int read() throws IOException {
		return b.hasRemaining() ? (b.get() & 0xFF) : -1;
	}

	@Override
	public int read(byte[] bytes, int off, int len) throws IOException {
		if (!b.hasRemaining()) return -1;
		len = Math.min(len, b.remaining());
		b.get(bytes, off, len);
		return len;
	}

}
