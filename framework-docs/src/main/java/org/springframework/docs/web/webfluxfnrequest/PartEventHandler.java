/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
