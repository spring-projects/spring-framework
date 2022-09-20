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

import org.springframework.lang.Nullable;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DynamicClassFileObject extends SimpleJavaFileObject {

	private final String className;

	@Nullable
	private volatile byte[] bytes;


	DynamicClassFileObject(String className) {
		super(createUri(className), Kind.CLASS);
		this.className = className;
	}

	DynamicClassFileObject(String className, byte[] bytes) {
		super(createUri(className), Kind.CLASS);
		this.className = className;
		this.bytes = bytes;
	}


	private static URI createUri(String className) {
		return URI.create("class:///" + className.replace('.', '/') + ".class");
	}

	@Override
	public InputStream openInputStream() throws IOException {
		byte[] content = this.bytes;
		if (content == null) {
			throw new IOException("No data written");
		}
		return new ByteArrayInputStream(content);
	}

	@Override
	public OutputStream openOutputStream() {
		return new JavaClassOutputStream();
	}

	private void closeOutputStream(byte[] bytes) {
		this.bytes = bytes;
	}

	String getClassName() {
		return this.className;
	}

	@Nullable
	byte[] getBytes() {
		return this.bytes;
	}


	class JavaClassOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() {
			closeOutputStream(toByteArray());
		}

	}

}
