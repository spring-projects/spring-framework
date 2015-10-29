/*
 * Copyright (c) 2011-2015 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.reactive.web.http.reactor;

import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.reactive.web.http.ServerHttpResponse;
import org.springframework.util.Assert;
import reactor.Publishers;
import reactor.io.buffer.Buffer;
import reactor.io.net.http.HttpChannel;
import reactor.io.net.http.model.Status;
import reactor.rx.Stream;
import reactor.rx.Streams;

import java.nio.ByteBuffer;

/**
 * @author Stephane Maldini
 */
public class ReactorServerHttpResponse extends PublisherReactorServerHttpResponse {

	public ReactorServerHttpResponse(HttpChannel<?, Buffer> response) {
		super(response);
	}

	@Override
	public Stream<Void> writeHeaders() {
		return Streams.wrap(super.writeHeaders());
	}

	@Override
	public Stream<Void> writeWith(Publisher<ByteBuffer> contentPublisher) {
		return Streams.wrap(super.writeWith(contentPublisher));
	}
}
