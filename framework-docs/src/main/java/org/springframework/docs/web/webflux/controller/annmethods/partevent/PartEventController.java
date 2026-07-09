/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
