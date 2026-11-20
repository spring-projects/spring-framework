/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webfluxfnrequest;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class PartEventHandler {

	public void handle(ServerRequest request) {
		// tag::snippet[]
		request.bodyToFlux(PartEvent.class).windowUntil(PartEvent::isLast)
				.concatMap(p -> p.switchOnFirst((signal, partEvents) -> {
					if (signal.hasValue()) {
						PartEvent event = signal.get();
						if (event instanceof FormPartEvent formEvent) {
							String value = formEvent.value();
							// handle form field
						}
						else if (event instanceof FilePartEvent fileEvent) {
							String filename = fileEvent.filename();
							Flux<DataBuffer> contents = partEvents.map(PartEvent::content);
							// handle file upload
						}
						else {
							return Mono.error(new RuntimeException("Unexpected event: " + event));
						}
					}
					else {
						return partEvents; // either complete or error signal
					}
					return Mono.empty();
				}));
		// end::snippet[]
	}
}
