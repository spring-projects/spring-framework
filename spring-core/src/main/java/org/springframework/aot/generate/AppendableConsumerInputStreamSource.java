/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.generate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.InputStreamSource;
import org.springframework.util.function.ThrowingConsumer;

/**
 * Adapter class to convert a {@link ThrowingConsumer} of {@link Appendable} to
 * an {@link InputStreamSource}.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class AppendableConsumerInputStreamSource implements InputStreamSource {

	private final ThrowingConsumer<Appendable> content;


	AppendableConsumerInputStreamSource(ThrowingConsumer<Appendable> content) {
		this.content = content;
	}


	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(toString().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		this.content.accept(buffer);
		return buffer.toString();
	}

}
