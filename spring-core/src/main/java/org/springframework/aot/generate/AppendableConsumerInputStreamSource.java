/*
 * Copyright 2002-2023 the original author or authors.
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
