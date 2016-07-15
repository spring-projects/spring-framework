/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.mock.http.client;

import java.io.IOException;
import java.net.URI;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.AsyncClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * An extension of {@link MockClientHttpRequest} that also implements
 * {@link AsyncClientHttpRequest} by wrapping the response in a
 * {@link SettableListenableFuture}.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class MockAsyncClientHttpRequest extends MockClientHttpRequest implements AsyncClientHttpRequest {

	public MockAsyncClientHttpRequest() {
	}

	public MockAsyncClientHttpRequest(HttpMethod httpMethod, URI uri) {
		super(httpMethod, uri);
	}


	@Override
	public ListenableFuture<ClientHttpResponse> executeAsync() throws IOException {
		SettableListenableFuture<ClientHttpResponse> future = new SettableListenableFuture<>();
		future.set(execute());
		return future;
	}

}
