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

package org.springframework.messaging.rsocket;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.lang.Nullable;

/**
 * {@link RSocket} that saves the name of the invoked method and the input payload(s).
 */
public class TestRSocket implements RSocket {

	private Mono<Payload> payloadMonoToReturn = Mono.empty();

	private Flux<Payload> payloadFluxToReturn = Flux.empty();

	@Nullable private volatile String savedMethodName;

	@Nullable private volatile Payload savedPayload;

	@Nullable private volatile Flux<Payload> savedPayloadFlux;


	public void setPayloadMonoToReturn(Mono<Payload> payloadMonoToReturn) {
		this.payloadMonoToReturn = payloadMonoToReturn;
	}

	public void setPayloadFluxToReturn(Flux<Payload> payloadFluxToReturn) {
		this.payloadFluxToReturn = payloadFluxToReturn;
	}

	@Nullable
	public String getSavedMethodName() {
		return this.savedMethodName;
	}

	@Nullable
	public Payload getSavedPayload() {
		return this.savedPayload;
	}

	@Nullable
	public Flux<Payload> getSavedPayloadFlux() {
		return this.savedPayloadFlux;
	}

	public void reset() {
		this.savedMethodName = null;
		this.savedPayload = null;
		this.savedPayloadFlux = null;
	}


	@Override
	public Mono<Void> fireAndForget(Payload payload) {
		this.savedMethodName = "fireAndForget";
		this.savedPayload = payload;
		return Mono.empty();
	}

	@Override
	public Mono<Payload> requestResponse(Payload payload) {
		this.savedMethodName = "requestResponse";
		this.savedPayload = payload;
		return this.payloadMonoToReturn;
	}

	@Override
	public Flux<Payload> requestStream(Payload payload) {
		this.savedMethodName = "requestStream";
		this.savedPayload = payload;
		return this.payloadFluxToReturn;
	}

	@Override
	public Flux<Payload> requestChannel(Publisher<Payload> publisher) {
		this.savedMethodName = "requestChannel";
		this.savedPayloadFlux = Flux.from(publisher);
		return this.payloadFluxToReturn;
	}

}
