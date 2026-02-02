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

package org.springframework.docs.web.webfluxfnrequest

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.codec.multipart.FilePartEvent
import org.springframework.http.codec.multipart.FormPartEvent
import org.springframework.http.codec.multipart.PartEvent
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.bodyToFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class PartEventHandler {

	fun handle(request: ServerRequest) {
		// tag::snippet[]
		request.bodyToFlux<PartEvent>().windowUntil(PartEvent::isLast)
			.concatMap {
				it.switchOnFirst { signal, partEvents ->
					if (signal.hasValue()) {
						val event = signal.get()
						if (event is FormPartEvent) {
							val value: String = event.value()
							// handle form field
						} else if (event is FilePartEvent) {
							val filename: String = event.filename()
							val contents: Flux<DataBuffer> = partEvents.map(PartEvent::content)
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
		// end::snippet[]
	}
}
