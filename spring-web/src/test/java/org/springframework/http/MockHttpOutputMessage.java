/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class MockHttpOutputMessage implements HttpOutputMessage {

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream body = new ByteArrayOutputStream();

	private boolean headersWritten = false;

	private final HttpHeaders writtenHeaders = new HttpHeaders();


	@Override
	public HttpHeaders getHeaders() {
		return (this.headersWritten ? HttpHeaders.readOnlyHttpHeaders(this.headers) : this.headers);
	}

	/**
	 * Return a copy of the actual headers written at the time of the call to
	 * getResponseBody, i.e. ignoring any further changes that may have been made to
	 * the underlying headers, e.g. via a previously obtained instance.
	 */
	public HttpHeaders getWrittenHeaders() {
		return writtenHeaders;
	}

	@Override
	public OutputStream getBody() throws IOException {
		writeHeaders();
		return body;
	}

	public byte[] getBodyAsBytes() {
		writeHeaders();
		return body.toByteArray();
	}

	public String getBodyAsString(Charset charset) {
		byte[] bytes = getBodyAsBytes();
		return new String(bytes, charset);
	}

	private void writeHeaders() {
		if (this.headersWritten) {
			return;
		}
		this.headersWritten = true;
		this.writtenHeaders.putAll(this.headers);
	}

}
