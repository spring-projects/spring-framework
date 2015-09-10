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

package org.springframework.reactive.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import reactor.rx.Streams;

/**
 * {@code OutputStream} implementation that stores all written bytes, to be retrieved
 * using {@link #toByteArrayPublisher()}.
 * @author Arjen Poutsma
 */
public class ByteArrayPublisherOutputStream extends OutputStream {

	private final List<byte[]> buffers = new ArrayList<>();


	/**
	 * Returns the written data as a {@code Publisher}.
	 * @return a publisher for the written bytes
	 */
	public Publisher<byte[]> toByteArrayPublisher() {
		return Streams.from(buffers);
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[]{(byte) b});
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		byte[] copy = new byte[len - off];
		System.arraycopy(b, off, copy, 0, len);
		buffers.add(copy);
	}

}
