/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.Mono;

import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * A combination of functions that can populate a {@link ReactiveHttpOutputMessage} body.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @param <T> the type of data to insert
 * @param <M> the type of {@link ReactiveHttpOutputMessage} this inserter can be applied to
 * @see BodyInserters
 */
@FunctionalInterface
public interface BodyInserter<T, M extends ReactiveHttpOutputMessage> {

	/**
	 * Insert into the given output message.
	 * @param outputMessage the response to insert into
	 * @param context the context to use
	 * @return a {@code Mono} that indicates completion or error
	 */
	Mono<Void> insert(M outputMessage, Context context);


	/**
	 * Defines the context used during the insertion.
	 */
	interface Context {

		/**
		 * Return the {@link HttpMessageWriter HttpMessageWriters} to be used for response body conversion.
		 * @return the stream of message writers
		 */
		List<HttpMessageWriter<?>> messageWriters();

		/**
		 * Optionally return the {@link ServerHttpRequest}, if present.
		 */
		Optional<ServerHttpRequest> serverRequest();

		/**
		 * Return the map of hints to use for response body conversion.
		 */
		Map<String, Object> hints();
	}

}
