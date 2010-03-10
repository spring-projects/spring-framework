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
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpOutputMessage;

/**
 * Implementation of {@link HttpOutputMessage} used for writing multipart data.
 *
 * @author Arjen Poutsma
 * @since 3.0.2
 */
class MultipartHttpOutputMessage implements HttpOutputMessage {

	private final HttpHeaders headers = new HttpHeaders();

	private final OutputStream os;

	private boolean headersWritten = false;

	public MultipartHttpOutputMessage(OutputStream os) {
		this.os = os;
	}

	public HttpHeaders getHeaders() {
		return headersWritten ? HttpHeaders.readOnlyHttpHeaders(headers) : this.headers;
	}

	public OutputStream getBody() throws IOException {
		writeHeaders();
		return this.os;
	}

	private void writeHeaders() throws IOException {
		if (!this.headersWritten) {
			for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
				byte[] headerName = getAsciiBytes(entry.getKey());
				for (String headerValueString : entry.getValue()) {
					byte[] headerValue = getAsciiBytes(headerValueString);
					os.write(headerName);
					os.write(':');
					os.write(' ');
					os.write(headerValue);
					writeNewLine(os);
				}
			}
			writeNewLine(os);
			this.headersWritten = true;
		}
	}

	private void writeNewLine(OutputStream os) throws IOException {
		os.write('\r');
		os.write('\n');
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


}
