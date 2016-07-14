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

package org.springframework.http;

import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.FlushingDataBuffer;

/**
 * A "reactive" HTTP output message that accepts output as a {@link Publisher}.
 *
 * <p>Typically implemented by an HTTP request on the client-side or a response
 * on the server-side.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 */
public interface ReactiveHttpOutputMessage extends HttpMessage {

	/**
	 * Register an action to be applied just before the message is committed.
	 * @param action the action
	 */
	void beforeCommit(Supplier<? extends Mono<Void>> action);

	/**
	 * Use the given {@link Publisher} to write the body of the message to the underlying
	 * HTTP layer, and flush the data when the complete signal is received (data could be
	 * flushed before depending on the configuration, the HTTP engine and the amount of
	 * data sent).
	 *
	 * <p>Each {@link FlushingDataBuffer} element will trigger a flush.
	 *
	 * @param body the body content publisher
	 * @return a publisher that indicates completion or error.
	 */
	Mono<Void> writeWith(Publisher<DataBuffer> body);

	/**
	 * Returns a {@link DataBufferFactory} that can be used for creating the body.
	 * @return a buffer factory
	 * @see #writeWith(Publisher)
	 */
	DataBufferFactory bufferFactory();

	/**
	 * Indicate that message handling is complete, allowing for any cleanup or
	 * end-of-processing tasks to be performed such as applying header changes
	 * made via {@link #getHeaders()} to the underlying HTTP message (if not
	 * applied already).
	 * <p>This method should be automatically invoked at the end of message
	 * processing so typically applications should not have to invoke it.
	 * If invoked multiple times it should have no side effects.
	 */
	Mono<Void> setComplete();

}
