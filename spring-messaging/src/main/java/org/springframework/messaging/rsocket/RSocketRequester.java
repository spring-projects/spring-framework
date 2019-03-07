/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.rsocket;

import io.rsocket.RSocket;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;

/**
 * A thin wrapper around a sending {@link RSocket} with a fluent API accepting
 * and returning higher level Objects for input and for output, along with
 * methods specify routing and other metadata.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public interface RSocketRequester {


	/**
	 * Return the underlying RSocket used to make requests.
	 */
	RSocket rsocket();


	/**
	 * Create a new {@code RSocketRequester} from the given {@link RSocket} and
	 * strategies for encoding and decoding request and response payloads.
	 * @param rsocket the sending RSocket to use
	 * @param dataMimeType the MimeType for data (from the SETUP frame)
	 * @param strategies encoders, decoders, and others
	 * @return the created RSocketRequester wrapper
	 */
	static RSocketRequester create(RSocket rsocket, @Nullable MimeType dataMimeType, RSocketStrategies strategies) {
		return new DefaultRSocketRequester(rsocket, dataMimeType, strategies);
	}


	// For now we treat metadata as a simple string that is the route.
	// This will change after the resolution of:
	// https://github.com/rsocket/rsocket-java/issues/568

	/**
	 * Entry point to prepare a new request to the given route.
	 *
	 * <p>For requestChannel interactions, i.e. Flux-to-Flux the metadata is
	 * attached to the first request payload.
	 *
	 * @param route the routing destination
	 * @return a spec for further defining and executing the reuqest
	 */
	RequestSpec route(String route);


	/**
	 * Contract to provide input data for an RSocket request.
	 */
	interface RequestSpec {

		/**
		 * Provide request payload data. The given Object may be a synchronous
		 * value, or a {@link Publisher} of values, or another async type that's
		 * registered in the configured {@link ReactiveAdapterRegistry}.
		 * <p>For multivalued Publishers, prefer using
		 * {@link #data(Publisher, Class)} or
		 * {@link #data(Publisher, ParameterizedTypeReference)} since that makes
		 * it possible to find a compatible {@code Encoder} up front vs looking
		 * it up on every value.
		 * @param data the Object to use for payload data
		 * @return spec for declaring the expected response
		 */
		ResponseSpec data(Object data);

		/**
		 * Provide a {@link Publisher} of value(s) for request payload data.
		 * <p>Publisher semantics determined through the configured
		 * {@link ReactiveAdapterRegistry} influence which of the 4 RSocket
		 * interactions to use. Publishers with unknown semantics are treated
		 * as multivalued. Consider registering a reactive type adapter, or
		 * passing {@code Mono.from(publisher)}.
		 * <p>If the publisher completes empty, possibly {@code Publisher<Void>},
		 * the request will have an empty data Payload.
		 * @param publisher source of payload data value(s)
		 * @param dataType the type of values to be published
		 * @param <T> the type of element values
		 * @param <P> the type of publisher
		 * @return spec for declaring the expected response
		 */
		<T, P extends Publisher<T>> ResponseSpec data(P publisher, Class<T> dataType);

		/**
		 * Variant of {@link #data(Publisher, Class)} for when the dataType has
		 * to have a generic type. See {@link ParameterizedTypeReference}.
		 */
		<T, P extends Publisher<T>> ResponseSpec data(P publisher, ParameterizedTypeReference<T> dataTypeRef);
	}


	/**
	 * Contract to declare the expected RSocket response.
	 */
	interface ResponseSpec {

		/**
		 * Perform {@link RSocket#fireAndForget fireAndForget}.
		 */
		Mono<Void> send();

		/**
		 * Perform {@link RSocket#requestResponse requestResponse}. If the
		 * expected data type is {@code Void.class}, the returned {@code Mono}
		 * will complete after all data is consumed.
		 * <p><strong>Note:</strong> Use of this method will raise an error if
		 * the request payload is a multivalued {@link Publisher} as
		 * determined through the configured {@link ReactiveAdapterRegistry}.
		 * @param dataType the expected data type for the response
		 * @param <T> parameter for the expected data type
		 * @return the decoded response
		 */
		<T> Mono<T> retrieveMono(Class<T> dataType);

		/**
		 * Variant of {@link #retrieveMono(Class)} for when the dataType has
		 * to have a generic type. See {@link ParameterizedTypeReference}.
		 */
		<T> Mono<T> retrieveMono(ParameterizedTypeReference<T> dataTypeRef);

		/**
		 * Perform {@link RSocket#requestStream requestStream} or
		 * {@link RSocket#requestChannel requestChannel} depending on whether
		 * the request input consists of a single or multiple payloads.
		 * If the expected data type is {@code Void.class}, the returned
		 * {@code Flux} will complete after all data is consumed.
		 * @param dataType the expected type for values in the response
		 * @param <T> parameterize the expected type of values
		 * @return the decoded response
		 */
		<T> Flux<T> retrieveFlux(Class<T> dataType);

		/**
		 * Variant of {@link #retrieveFlux(Class)} for when the dataType has
		 * to have a generic type. See {@link ParameterizedTypeReference}.
		 */
		<T> Flux<T> retrieveFlux(ParameterizedTypeReference<T> dataTypeRef);
	}

}
