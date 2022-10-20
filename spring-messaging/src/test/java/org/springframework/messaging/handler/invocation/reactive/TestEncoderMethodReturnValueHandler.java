/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.messaging.handler.invocation.reactive;

import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.MethodParameter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.Message;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Implementation of {@link AbstractEncoderMethodReturnValueHandler} for tests.
 * "Handles" by storing encoded return values.
 *
 * @author Rossen Stoyanchev
 */
public class TestEncoderMethodReturnValueHandler extends AbstractEncoderMethodReturnValueHandler {

	private Flux<DataBuffer> encodedContent;


	public TestEncoderMethodReturnValueHandler(List<Encoder<?>> encoders, ReactiveAdapterRegistry registry) {
		super(encoders, registry);
	}


	public Flux<DataBuffer> getContent() {
		return this.encodedContent;
	}

	public Flux<String> getContentAsStrings() {
		return this.encodedContent.map(buffer -> buffer.toString(UTF_8));
	}

	@Override
	protected Mono<Void> handleEncodedContent(
			Flux<DataBuffer> encodedContent, MethodParameter returnType, Message<?> message) {

		this.encodedContent = encodedContent.cache();
		return this.encodedContent.then();
	}

	@Override
	protected Mono<Void> handleNoContent(MethodParameter returnType, Message<?> message) {
		this.encodedContent = Flux.empty();
		return Mono.empty();
	}

}
