/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.nio.file.Path;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Represents an event for a "multipart/form-data" request.
 * Can be a {@link FormPartEvent} or a {@link FilePartEvent}.
 *
 * <h2>Server Side</h2>
 *
 * Each part in a multipart HTTP message produces at least one
 * {@code PartEvent} containing both {@link #headers() headers} and a
 * {@linkplain PartEvent#content() buffer} with the contents of the part.
 * <ul>
 * <li>Form fields will produce a <em>single</em> {@link FormPartEvent},
 * containing the {@linkplain FormPartEvent#value() value} of the field.</li>
 * <li>File uploads will produce <em>one or more</em> {@link FilePartEvent}s,
 * containing the {@linkplain FilePartEvent#filename() filename} used when
 * uploading. If the file is large enough to be split across multiple buffers,
 * the first {@code FilePartEvent} will be followed by subsequent events.</li>
 * </ul>
 * The final {@code PartEvent} for a particular part will have
 * {@link #isLast()} set to {@code true}, and can be followed by
 * additional events belonging to subsequent parts.
 * The {@code isLast()} property is suitable as a predicate for the
 * {@link Flux#windowUntil(Predicate)} operator, in order to split events from
 * all parts into windows that each belong to a single part.
 * From that, the {@link Flux#switchOnFirst(BiFunction)} operator allows you to
 * see whether you are handling a form field or file upload.
 * For example:
 *
 * <pre class=code>
 * Flux&lt;PartEvent&gt; allPartsEvents = ... // obtained via @RequestPayload or request.bodyToFlux(PartEvent.class)
 * allPartsEvents.windowUntil(PartEvent::isLast)
 *   .concatMap(p -> p.switchOnFirst((signal, partEvents) -> {
 *       if (signal.hasValue()) {
 *           PartEvent event = signal.get();
 *           if (event instanceof FormPartEvent formEvent) {
 *               String value = formEvent.value();
 *               // handle form field
 *           }
 *           else if (event instanceof FilePartEvent fileEvent) {
 *               String filename = fileEvent.filename();
 *               Flux&lt;DataBuffer&gt; contents = partEvents.map(PartEvent::content);
 *               // handle file upload
 *           }
 *           else {
 *               return Mono.error(new RuntimeException("Unexpected event: " + event));
 *           }
 *       }
 *       else {
 *         return partEvents; // either complete or error signal
 *       }
 *   }))
 * </pre>
 * Received part events can also be relayed to another service by using the
 * {@link org.springframework.web.reactive.function.client.WebClient WebClient}.
 * See below.
 *
 * <p><strong>NOTE</strong> that the {@linkplain PartEvent#content() body contents}
 * must be completely consumed, relayed, or released to avoid memory leaks.
 *
 * <h2>Client Side</h2>
 * On the client side, {@code PartEvent}s can be created to represent a file upload.
 * <ul>
 * <li>Form fields can be created via {@link FormPartEvent#create(String, String)}.</li>
 * <li>File uploads can be created via {@link FilePartEvent#create(String, Path)}.</li>
 * </ul>
 * The streams returned by these static methods can be concatenated via
 * {@link Flux#concat(Publisher[])} to create a request for the
 * {@link org.springframework.web.reactive.function.client.WebClient WebClient}:
 * For instance, this sample will POST a multipart form containing a form field
 * and a file.
 *
 * <pre class=code>
 * Resource resource = ...
 * Mono&lt;String&gt; result = webClient
 *   .post()
 *   .uri("https://example.com")
 *   .body(Flux.concat(
 *     FormPartEvent.create("field", "field value"),
 *     FilePartEvent.create("file", resource)
 *   ), PartEvent.class)
 *   .retrieve()
 *   .bodyToMono(String.class);
 * </pre>
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see FormPartEvent
 * @see FilePartEvent
 * @see PartEventHttpMessageReader
 * @see PartEventHttpMessageWriter
 */
public interface PartEvent {

	/**
	 * Return the name of the event, as provided through the
	 * {@code Content-Disposition name} parameter.
	 * @return the name of the part, never {@code null} or empty
	 */
	default String name() {
		String name = headers().getContentDisposition().getName();
		Assert.state(name != null, "No name available");
		return name;
	}

	/**
	 * Return the headers of the part that this event belongs to.
	 */
	HttpHeaders headers();

	/**
	 * Return the content of this event. The returned buffer must be consumed or
	 * {@linkplain org.springframework.core.io.buffer.DataBufferUtils#release(DataBuffer) released}.
	 */
	DataBuffer content();

	/**
	 * Indicates whether this is the last event of a particular
	 * part.
	 */
	boolean isLast();

}
