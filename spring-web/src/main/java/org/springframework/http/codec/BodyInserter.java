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

package org.springframework.http.codec;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Stream;

import reactor.core.publisher.Mono;

import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.util.Assert;

/**
 * A combination of functions that can populate a {@link ReactiveHttpOutputMessage} body.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface BodyInserter<T, M extends ReactiveHttpOutputMessage> {

	/**
	 * Insert into the given response.
	 * @param outputMessage the response to insert into
	 * @param context the context to use
	 * @return a {@code Mono} that indicates completion or error
	 */
	Mono<Void> insert(M outputMessage, Context context);

	/**
	 * Return the type contained in the body.
	 * @return the type contained in the body
	 */
	T t();

	/**
	 * Return a new {@code BodyInserter} described by the given writer and supplier functions.
	 * @param writer  the writer function for the new inserter
	 * @param supplier the supplier function for the new inserter
	 * @param <T> the type supplied and written by the inserter
	 * @return the new {@code BodyInserter}
	 */
	static <T, M extends ReactiveHttpOutputMessage> BodyInserter<T, M> of(
			BiFunction<M, Context, Mono<Void>> writer,
			Supplier<T> supplier) {

		Assert.notNull(writer, "'writer' must not be null");
		Assert.notNull(supplier, "'supplier' must not be null");

		return new BodyInserters.DefaultBodyInserter<T, M>(writer, supplier);
	}

	/**
	 * Defines the context used during the insertion.
	 */
	interface Context {

		/**
		 * Supply a {@linkplain Stream stream} of {@link HttpMessageWriter}s to be used for response
		 * body conversion.
		 * @return the stream of message writers
		 */
		Supplier<Stream<HttpMessageWriter<?>>> messageWriters();

	}


}
