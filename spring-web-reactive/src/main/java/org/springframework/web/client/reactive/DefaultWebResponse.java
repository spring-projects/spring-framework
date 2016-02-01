/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.client.reactive;

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.core.codec.Decoder;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Default implementation of the {@link WebResponse} interface
 *
 * @author Brian Clozel
 */
public class DefaultWebResponse implements WebResponse {

	private final Mono<ClientHttpResponse> clientResponse;

	private final List<Decoder<?>> messageDecoders;


	public DefaultWebResponse(Mono<ClientHttpResponse> clientResponse, List<Decoder<?>> messageDecoders) {
		this.clientResponse = clientResponse;
		this.messageDecoders = messageDecoders;
	}

	@Override
	public Mono<ClientHttpResponse> getClientResponse() {
		return this.clientResponse;
	}

	@Override
	public List<Decoder<?>> getMessageDecoders() {
		return this.messageDecoders;
	}
}
