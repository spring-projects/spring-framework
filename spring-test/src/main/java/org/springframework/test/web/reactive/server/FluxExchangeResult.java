/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import reactor.core.publisher.Flux;

/**
 * {@code ExchangeResult} variant with the response body decoded as
 * {@code Flux<T>} but not yet consumed.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <T> the type of elements in the response body
 * @see EntityExchangeResult
 */
public class FluxExchangeResult<T> extends ExchangeResult {

	private final Flux<T> body;


	FluxExchangeResult(ExchangeResult result, Flux<T> body) {
		super(result);
		this.body = body;
	}


	/**
	 * Return the response body as a {@code Flux<T>} of decoded elements.
	 *
	 * <p>The response body stream can then be consumed further with the
	 * "reactor-test" {@code StepVerifier} and cancelled when enough elements have been
	 * consumed from the (possibly infinite) stream:
	 *
	 * <pre>
	 * FluxExchangeResult&lt;Person&gt; result = this.client.get()
	 * 	.uri("/persons")
	 * 	.accept(TEXT_EVENT_STREAM)
	 * 	.exchange()
	 * 	.expectStatus().isOk()
	 * 	.expectHeader().contentType(TEXT_EVENT_STREAM)
	 * 	.expectBody(Person.class)
	 * 	.returnResult();
	 *
	 * StepVerifier.create(result.getResponseBody())
	 * 	.expectNext(new Person("Jane"), new Person("Jason"))
	 * 	.expectNextCount(4)
	 * 	.expectNext(new Person("Jay"))
	 * 	.thenCancel()
	 * 	.verify();
	 * </pre>
	 */
	public Flux<T> getResponseBody() {
		return this.body;
	}

	/**
	 * Invoke the given consumer within {@link #assertWithDiagnostics(Runnable)}
	 * passing {@code "this"} instance to it. This method allows the following,
	 * without leaving the {@code WebTestClient} chain of calls:
	 * <pre class="code">
	 *	client.get()
	 * 		.uri("/persons")
	 * 		.accept(TEXT_EVENT_STREAM)
	 * 		.exchange()
	 * 		.expectStatus().isOk()
	 *	 	.returnResult()
	 *	 	.consumeWith(result -> assertThat(...);
	 * </pre>
	 * @param consumer consumer for {@code "this"} instance
	 */
	public void consumeWith(Consumer<FluxExchangeResult<T>> consumer) {
		assertWithDiagnostics(() -> consumer.accept(this));
	}

}
