/*
 * Copyright 2002-2015 the original author or authors.
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

import java.nio.ByteBuffer;

import org.reactivestreams.Publisher;

/**
 * Represents a "reactive" HTTP output message, consisting of {@linkplain #getHeaders() headers}
 * and the capability to add a {@linkplain #setBody(Publisher) body}.
 *
 * <p>Typically implemented by an HTTP request on the client-side, or a response on the server-side.
 *
 * @author Arjen Poutsma
 */
public interface ReactiveHttpOutputMessage extends HttpMessage {

	/**
	 * Adds the given publisher of {@link ByteBuffer}s as a body. A HTTP/1.1 message has
	 * one body, but HTTP/1.2 supports multiple bodies.
	 * @param body the body to add
	 * @return a publisher that indicates completion
	 */
	Publisher<Void> setBody(Publisher<ByteBuffer> body);

}
