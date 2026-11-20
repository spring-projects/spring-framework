/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;

/**
 * A "reactive" HTTP input message that exposes the input as {@link Publisher}.
 *
 * <p>Typically implemented by an HTTP request on the server-side or a response
 * on the client-side.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public interface ReactiveHttpInputMessage extends HttpMessage {

	/**
	 * Return the body of the message as a {@link Publisher}.
	 * @return the body content publisher
	 */
	Flux<DataBuffer> getBody();

}
