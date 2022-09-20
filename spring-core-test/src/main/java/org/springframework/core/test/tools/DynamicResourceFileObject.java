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

package org.springframework.core.test.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * In-memory {@link JavaFileObject} used to hold generated resource file contents.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 6.0
 */
class DynamicResourceFileObject extends SimpleJavaFileObject {

	private volatile byte[] bytes;


	DynamicResourceFileObject(String fileName) {
		super(createUri(fileName), Kind.OTHER);
	}

	DynamicResourceFileObject(String fileName, String content) {
		super(createUri(fileName), Kind.OTHER);
		this.bytes = content.getBytes();
	}


	private static URI createUri(String fileName) {
		return URI.create("resource:///" + fileName);
	}

	@Override
	public InputStream openInputStream() throws IOException {
		if (this.bytes == null) {
			throw new IOException("No data written");
		}
		return new ByteArrayInputStream(this.bytes);
	}

	@Override
	public OutputStream openOutputStream() {
		return new JavaResourceOutputStream();
	}

	private void closeOutputStream(byte[] bytes) {
		this.bytes = bytes;
	}

	byte[] getBytes() {
		return this.bytes;
	}


	class JavaResourceOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() {
			closeOutputStream(toByteArray());
		}

	}

}
