/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.http.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.springframework.util.Assert;
import org.springframework.http.HttpHeaders;

/**
 * Abstract base for {@link ClientHttpRequest} that makes sure that headers and body are not written multiple times.
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	private boolean executed = false;

	private final HttpHeaders headers = new HttpHeaders();

	private final ByteArrayOutputStream bufferedOutput = new ByteArrayOutputStream();


	public final HttpHeaders getHeaders() {
		checkExecuted();
		return this.headers;
	}

	public final OutputStream getBody() throws IOException {
		checkExecuted();
		return this.bufferedOutput;
	}

	public final ClientHttpResponse execute() throws IOException {
		checkExecuted();
		ClientHttpResponse result = executeInternal(this.headers, this.bufferedOutput.toByteArray());
		this.executed = true;
		return result;
	}

	private void checkExecuted() {
		Assert.state(!this.executed, "ClientHttpRequest already executed");
	}


	/**
	 * Abstract template method that writes the given headers and content to the HTTP request.
	 * @param headers the HTTP headers
	 * @param bufferedOutput the body content
	 * @return the response object for the executed request
	 */
	protected abstract ClientHttpResponse executeInternal(HttpHeaders headers, byte[] bufferedOutput)
			throws IOException;

}
