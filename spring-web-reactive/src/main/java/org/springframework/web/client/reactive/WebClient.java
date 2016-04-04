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

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import reactor.core.publisher.Mono;

import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.support.ByteBufferDecoder;
import org.springframework.core.codec.support.ByteBufferEncoder;
import org.springframework.core.codec.support.JacksonJsonDecoder;
import org.springframework.core.codec.support.JacksonJsonEncoder;
import org.springframework.core.codec.support.JsonObjectDecoder;
import org.springframework.core.codec.support.StringDecoder;
import org.springframework.core.codec.support.StringEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Reactive Web client supporting the HTTP/1.1 protocol
 *
 * <p>Here is a simple example of a GET request:
 * <pre class="code">
 * WebClient client = new WebClient(new ReactorHttpClientRequestFactory());
 * Mono&lt;String&gt; result = client
 * 		.perform(HttpRequestBuilders.get("http://example.org/resource")
 * 			.accept(MediaType.TEXT_PLAIN))
 * 		.extract(WebResponseExtractors.body(String.class));
 * </pre>
 *
 * <p>This Web client relies on
 * <ul>
 *     <li>a {@link ClientHttpRequestFactory} that drives the underlying library (e.g. Reactor-Net, RxNetty...)</li>
 *     <li>an {@link HttpRequestBuilder} which create a Web request with a builder API (see {@link HttpRequestBuilders})</li>
 *     <li>an {@link WebResponseExtractor} which extracts the relevant part of the server response
 *     with the composition API of choice (see {@link WebResponseExtractors}</li>
 * </ul>
 *
 * @author Brian Clozel
 * @see HttpRequestBuilders
 * @see WebResponseExtractors
 */
public final class WebClient {

	private ClientHttpRequestFactory requestFactory;

	private List<Encoder<?>> messageEncoders;

	private List<Decoder<?>> messageDecoders;

	/**
	 * Create a {@code ReactiveRestClient} instance, using the {@link ClientHttpRequestFactory}
	 * implementation given as an argument to drive the underlying HTTP client implementation.
	 *
	 * Register by default the following Encoders and Decoders:
	 * <ul>
	 *     <li>{@link ByteBufferEncoder} / {@link ByteBufferDecoder}</li>
	 *     <li>{@link StringEncoder} / {@link StringDecoder}</li>
	 *     <li>{@link JacksonJsonEncoder} / {@link JacksonJsonDecoder}</li>
	 * </ul>
	 *
	 * @param requestFactory the {@code ClientHttpRequestFactory} to use
	 */
	public WebClient(ClientHttpRequestFactory requestFactory) {
		this.requestFactory = requestFactory;
		this.messageEncoders = Arrays.asList(new ByteBufferEncoder(), new StringEncoder(),
				new JacksonJsonEncoder());
		this.messageDecoders = Arrays.asList(new ByteBufferDecoder(), new StringDecoder(),
				new JacksonJsonDecoder(new JsonObjectDecoder()));
	}

	/**
	 * Set the list of {@link Encoder}s to use for encoding messages
	 */
	public void setMessageEncoders(List<Encoder<?>> messageEncoders) {
		this.messageEncoders = messageEncoders;
	}

	/**
	 * Set the list of {@link Decoder}s to use for decoding messages
	 */
	public void setMessageDecoders(List<Decoder<?>> messageDecoders) {
		this.messageDecoders = messageDecoders;
	}

	/**
	 * Perform the actual HTTP request/response exchange
	 *
	 * <p>Pulling demand from the exposed {@code Flux} will result in:
	 * <ul>
	 *     <li>building the actual HTTP request using the provided {@code RequestBuilder}</li>
	 *     <li>encoding the HTTP request body with the configured {@code Encoder}s</li>
	 *     <li>returning the response with a publisher of the body</li>
	 * </ul>
	 */
	public WebResponseActions perform(HttpRequestBuilder builder) {

		ClientHttpRequest request = builder.build(this.requestFactory, this.messageEncoders);
		final Mono<ClientHttpResponse> clientResponse = request.execute()
				.log("org.springframework.http.client.reactive");

		return new WebResponseActions() {
			@Override
			public void doWithStatus(Consumer<HttpStatus> consumer) {
				// TODO: implement
			}

			@Override
			public <T> T extract(WebResponseExtractor<T> extractor) {
				return extractor.extract(new DefaultWebResponse(clientResponse, messageDecoders));
			}

		};
	}

}
