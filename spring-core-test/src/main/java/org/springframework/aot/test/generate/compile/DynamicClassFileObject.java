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

package org.springframework.aot.test.generate.compile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DynamicClassFileObject extends SimpleJavaFileObject {

	private static final byte[] NO_BYTES = new byte[0];

	private final String className;

	private volatile byte[] bytes;


	DynamicClassFileObject(String className) {
		this(className, NO_BYTES);
	}

	DynamicClassFileObject(String className, byte[] bytes) {
		super(URI.create("class:///" + className.replace('.', '/') + ".class"), Kind.CLASS);
		this.className = className;
		this.bytes = bytes;
	}


	String getClassName() {
		return this.className;
	}

	byte[] getBytes() {
		return this.bytes;
	}

	@Override
	public InputStream openInputStream() throws IOException {
		return new ByteArrayInputStream(this.bytes);
	}

	@Override
	public OutputStream openOutputStream() {
		return new JavaClassOutputStream();
	}


	class JavaClassOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() {
			DynamicClassFileObject.this.bytes = toByteArray();
		}

	}

}
