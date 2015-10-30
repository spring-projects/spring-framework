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
 * Represents a "reactive" HTTP input message, consisting of
 * {@linkplain #getHeaders() headers} and a readable
 * {@linkplain #getBody() streaming body }.
 *
 * <p>Typically implemented by an HTTP request on the server-side, or a response
 * on the client-side.
 *
 * @author Arjen Poutsma
 */
public interface ReactiveHttpInputMessage extends HttpMessage {

	/**
	 * Return the body of the message as an publisher of {@code ByteBuffer}s.
	 * @return the body
	 */
	Publisher<ByteBuffer> getBody();

}
