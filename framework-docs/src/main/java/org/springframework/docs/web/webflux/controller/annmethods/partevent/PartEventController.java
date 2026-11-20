/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.web.webflux.controller.annmethods.partevent;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePartEvent;
import org.springframework.http.codec.multipart.FormPartEvent;
import org.springframework.http.codec.multipart.PartEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class PartEventController {

	// tag::snippet[]
	@PostMapping("/")
	public void handle(@RequestBody Flux<PartEvent> allPartEvents) {

		//	The final PartEvent for a particular part will have isLast() set to true, and can be
		//	followed by additional events belonging to subsequent parts.
		//	This makes the isLast property suitable as a predicate for the Flux::windowUntil operator, to
		//	split events from all parts into windows that each belong to a single part.
		allPartEvents.windowUntil(PartEvent::isLast)
				//	The Flux::switchOnFirst operator allows you to see whether you are handling
				//	a form field or file upload
				.concatMap(p -> p.switchOnFirst((signal, partEvents) -> {
						if (signal.hasValue()) {
								PartEvent event = signal.get();
								if (event instanceof FormPartEvent formEvent) {
										String value = formEvent.value();
										// Handling of the form field
								}
								else if (event instanceof FilePartEvent fileEvent) {
										String filename = fileEvent.filename();

										// The body contents must be completely consumed, relayed, or released to avoid memory leaks
										Flux<DataBuffer> contents = partEvents.map(PartEvent::content);
										// Handling of the file upload
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
	}
	// end::snippet[]

}
