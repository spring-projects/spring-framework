/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import org.springframework.http.MediaType;
import org.springframework.util.Assert;

/**
 * <p>Inspired by {@link org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity}.
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 */
abstract class AbstractPart implements Part {

	private static final byte[] CONTENT_DISPOSITION =
			new byte[]{'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'D', 'i', 's', 'p', 'o', 's', 'i', 't', 'i', 'o', 'n',
					':', ' ', 'f', 'o', 'r', 'm', '-', 'd', 'a', 't', 'a', ';', ' ', 'n', 'a', 'm', 'e', '='};

	private static final byte[] CONTENT_TYPE =
			new byte[]{'C', 'o', 'n', 't', 'e', 'n', 't', '-', 'T', 'y', 'p', 'e', ':', ' '};

	private final MediaType contentType;

	protected AbstractPart(MediaType contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	public final void write(byte[] boundary, String name, OutputStream os) throws IOException {
		writeBoundary(boundary, os);
		writeContentDisposition(name, os);
		writeContentType(os);
		writeEndOfHeader(os);
		writeData(os);
		writeEnd(os);
	}

	protected void writeBoundary(byte[] boundary, OutputStream os) throws IOException {
		os.write('-');
		os.write('-');
		os.write(boundary);
		writeNewLine(os);
	}

	protected void writeContentDisposition(String name, OutputStream os) throws IOException {
		os.write(CONTENT_DISPOSITION);
		os.write('"');
		os.write(getAsciiBytes(name));
		os.write('"');
	}

	protected void writeContentType(OutputStream os) throws IOException {
		writeNewLine(os);
		os.write(CONTENT_TYPE);
		os.write(getAsciiBytes(contentType.toString()));
	}

	protected byte[] getAsciiBytes(String name) {
		try {
			return name.getBytes("US-ASCII");
		}
		catch (UnsupportedEncodingException ex) {
			// should not happen, US-ASCII is always supported
			throw new IllegalStateException(ex);
		}
	}

	protected void writeEndOfHeader(OutputStream os) throws IOException {
		writeNewLine(os);
		writeNewLine(os);
	}

	protected void writeEnd(OutputStream os) throws IOException {
		writeNewLine(os);
	}

	private void writeNewLine(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
	}

	protected abstract void writeData(OutputStream os) throws IOException;

}
