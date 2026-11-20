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
 * In-memory {@link JavaFileObject} used to hold generated resource file contents.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 6.0
 */
class DynamicResourceFileObject extends SimpleJavaFileObject {

	private volatile byte @Nullable [] bytes;


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
		byte[] content = this.bytes;
		if (content == null) {
			throw new IOException("No data written");
		}
		return new ByteArrayInputStream(content);
	}

	@Override
	public OutputStream openOutputStream() {
		return new JavaResourceOutputStream();
	}

	private void closeOutputStream(byte[] bytes) {
		this.bytes = bytes;
	}

	byte @Nullable [] getBytes() {
		return this.bytes;
	}


	class JavaResourceOutputStream extends ByteArrayOutputStream {

		@Override
		public void close() {
			closeOutputStream(toByteArray());
		}

	}

}
