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

package org.springframework.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.util.Assert;

/**
 * @author Arjen Poutsma
 */
public class MockHttpInputMessage implements HttpInputMessage {

	private final HttpHeaders headers = new HttpHeaders();

	private final InputStream body;

	public MockHttpInputMessage(byte[] contents) {
		Assert.notNull(contents, "'contents' must not be null");
		this.body = new ByteArrayInputStream(contents);
	}

	public MockHttpInputMessage(InputStream body) {
		Assert.notNull(body, "'body' must not be null");
		this.body = body;
	}

	public HttpHeaders getHeaders() {
		return headers;
	}

	public InputStream getBody() throws IOException {
		return body;
	}
}
