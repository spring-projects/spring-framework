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

package org.springframework.docs.web.webflux.controller.annmethods.partevent

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePartEvent
import org.springframework.http.codec.multipart.FormPartEvent
import org.springframework.http.codec.multipart.PartEvent
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class PartEventController {

	// tag::snippet[]
	@PostMapping("/")
	fun handle(@RequestBody allPartsEvents: Flux<PartEvent>) { // <1>
		allPartsEvents.windowUntil(PartEvent::isLast) // <2>
				.concatMap {
					it.switchOnFirst { signal, partEvents -> // <3>
						if (signal.hasValue()) {
							val event = signal.get()
							if (event is FormPartEvent) { // <4>
								val value: String = event.value()
								// handle form field
							} else if (event is FilePartEvent) { // <5>
								val filename: String = event.filename()
								val contents: Flux<DataBuffer> = partEvents.map(PartEvent::content) // <6>
								// handle file upload
							} else {
								return@switchOnFirst Mono.error(RuntimeException("Unexpected event: $event"))
							}
						} else {
							return@switchOnFirst partEvents // either complete or error signal
						}
						Mono.empty()
					}
				}
	}
	// end::snippet[]

}