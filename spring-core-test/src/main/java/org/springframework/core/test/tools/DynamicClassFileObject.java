/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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

import org.jspecify.annotations.Nullable;

/**
 * In-memory {@link JavaFileObject} used to hold class bytecode.
 *
 * @author Phillip Webb
 * @since 6.0
 */
class DynamicClassFileObject extends SimpleJavaFileObject {

	private final String className;

	private volatile byte @Nullable [] bytes;


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

	byte @Nullable [] getBytes() {
		return this.bytes;
	}


	class JavaClassOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() {
			closeOutputStream(toByteArray());
		}

	}

}
