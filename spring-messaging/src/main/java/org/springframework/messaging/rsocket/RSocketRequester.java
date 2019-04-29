/*
 * Copyright 2002-2019 the original author or authors.
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

import java.net.URI;
import java.util.function.Consumer;

import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.transport.ClientTransport;
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
 * @author Brian Clozel
 * @since 5.2
 */
public interface RSocketRequester {

	/**
	 * Return the underlying RSocket used to make requests.
	 */
	RSocket rsocket();

	// For now we treat metadata as a simple string that is the route.
	// This will change after the resolution of:
	// https://github.com/rsocket/rsocket-java/issues/568

	/**
	 * Entry point to prepare a new request to the given route.
	 * <p>For requestChannel interactions, i.e. Flux-to-Flux the metadata is
	 * attached to the first request payload.
	 * @param route the routing destination
	 * @return a spec for further defining and executing the reuqest
	 */
	RequestSpec route(String route);


	/**
	 * Obtain a builder for an {@link RSocketRequester} by connecting to an
	 * RSocket server. The builder allows for customization of
	 * {@link RSocketFactory.ClientRSocketFactory ClientRSocketFactory} settings,
	 * {@link RSocketStrategies}, and for selecting the transport to use.
	 */
	static RSocketRequester.Builder builder() {
		return new DefaultRSocketRequesterBuilder();
	}

	/**
	 * Wrap an existing {@link RSocket}. Typically used in a client or server
	 * responder to wrap the remote {@code RSocket}.
	 * @param rsocket the RSocket to wrap
	 * @param dataMimeType the data MimeType, obtained from the
	 * {@link io.rsocket.ConnectionSetupPayload} (server) or the
	 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory} (client)
	 * @param strategies the strategies to use
	 * @return the created RSocketRequester
	 */
	static RSocketRequester wrap(RSocket rsocket, @Nullable MimeType dataMimeType, RSocketStrategies strategies) {
		return new DefaultRSocketRequester(rsocket, dataMimeType, strategies);
	}

	/**
	 * Create a new {@code RSocketRequester} from the given {@link RSocket} and
	 * strategies for encoding and decoding request and response payloads.
	 * @param rsocket the sending RSocket to use
	 * @param dataMimeType the MimeType for data (from the SETUP frame)
	 * @param strategies encoders, decoders, and others
	 * @return the created RSocketRequester wrapper
	 * @deprecated use {@link #wrap(RSocket, MimeType, RSocketStrategies)} instead
	 */
	@Deprecated
	static RSocketRequester create(RSocket rsocket, @Nullable MimeType dataMimeType, RSocketStrategies strategies) {
		return new DefaultRSocketRequester(rsocket, dataMimeType, strategies);
	}


	/**
	 * Builder to prepare an {@link RSocketRequester} by connecting to an
	 * RSocket server and wrapping the resulting {@link RSocket}.
	 */
	interface Builder {

		/**
		 * Configure the {@code ClientRSocketFactory}.
		 * <p>Note there is typically no need to set a data MimeType explicitly.
		 * By default a data MimeType is picked by taking the first concrete
		 * MimeType supported by the configured encoders and decoders.
		 * @param configurer the configurer to apply
		 */
		RSocketRequester.Builder rsocketFactory(Consumer<RSocketFactory.ClientRSocketFactory> configurer);

		/**
		 * Set the {@link RSocketStrategies} instance.
		 * @param strategies the strategies to use
		 */
		RSocketRequester.Builder rsocketStrategies(@Nullable RSocketStrategies strategies);

		/**
		 * Customize the {@link RSocketStrategies}.
		 * <p>By default this starts out with an empty builder, i.e.
		 * {@link RSocketStrategies#builder()}, but the strategies can also be
		 * set via {@link #rsocketStrategies(RSocketStrategies)}.
		 * @param configurer the configurer to apply
		 */
		RSocketRequester.Builder rsocketStrategies(Consumer<RSocketStrategies.Builder> configurer);

		/**
		 * Connect to the RSocket server over TCP.
		 * @param host the server host
		 * @param port the server port
		 * @return an {@code RSocketRequester} for the connection
		 */
		Mono<RSocketRequester> connectTcp(String host, int port);

		/**
		 * Connect to the RSocket server over WebSocket.
		 * @param uri the RSocket server endpoint URI
		 * @return an {@code RSocketRequester} for the connection
		 */
		Mono<RSocketRequester> connectWebSocket(URI uri);

		/**
		 * Connect to the RSocket server with the given {@code ClientTransport}.
		 * @param transport the client transport to use
		 * @return an {@code RSocketRequester} for the connection
		 */
		Mono<RSocketRequester> connect(ClientTransport transport);

	}


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
