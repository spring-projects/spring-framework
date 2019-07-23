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

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.transport.ClientTransport;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Decoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.rsocket.annotation.support.AnnotationClientResponderConfigurer;
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

	/**
	 * Return the data {@code MimeType} selected for the underlying RSocket
	 * at connection time. On the client side this is configured via
	 * {@link RSocketRequester.Builder#dataMimeType(MimeType)} while on the
	 * server side it's obtained from the {@link ConnectionSetupPayload}.
	 */
	MimeType dataMimeType();

	/**
	 * Return the metadata {@code MimeType} selected for the underlying RSocket
	 * at connection time. On the client side this is configured via
	 * {@link RSocketRequester.Builder#metadataMimeType(MimeType)} while on the
	 * server side it's obtained from the {@link ConnectionSetupPayload}.
	 */
	MimeType metadataMimeType();

	/**
	 * Begin to specify a new request with the given route to a remote handler.
	 * <p>The route can be a template with placeholders, e.g.
	 * {@code "flight.{code}"} in which case the supplied route variables are
	 * expanded into the template after being formatted via {@code toString()}.
	 * If a formatted variable contains a "." it is replaced with the escape
	 * sequence "%2E" to avoid treating it as separator by the responder .
	 * <p>If the connection is set to use composite metadata, the route is
	 * encoded as {@code "message/x.rsocket.routing.v0"}. Otherwise the route
	 * is encoded according to the mime type for the connection.
	 * @param route the route to a handler
	 * @param routeVars variables to be expanded into the route template
	 * @return a spec for further defining and executing the request
	 */
	RequestSpec route(String route, Object... routeVars);

	/**
	 * Begin to specify a new request with the given metadata.
	 * <p>If using composite metadata then the mime type argument is required.
	 * Otherwise the mime type should be {@code null}, or it must match the
	 * mime type for the connection.
	 * @param metadata the metadata value to encode
	 * @param mimeType the mime type that describes the metadata;
	 */
	RequestSpec metadata(Object metadata, @Nullable MimeType mimeType);


	/**
	 * Obtain a builder for an {@link RSocketRequester} by connecting to an
	 * RSocket server. The builder allows for customization of
	 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory ClientRSocketFactory}
	 * settings, {@link RSocketStrategies}, and for selecting the transport to use.
	 */
	static RSocketRequester.Builder builder() {
		return new DefaultRSocketRequesterBuilder();
	}

	/**
	 * Wrap an existing {@link RSocket}. This is typically used in a responder,
	 * client or server, to wrap the remote/sending {@code RSocket}.
	 * @param rsocket the RSocket to wrap
	 * @param dataMimeType the data MimeType from the {@code ConnectionSetupPayload}
	 * @param metadataMimeType the metadata MimeType from the {@code ConnectionSetupPayload}
	 * @param strategies the strategies to use
	 * @return the created RSocketRequester
	 */
	static RSocketRequester wrap(
			RSocket rsocket, MimeType dataMimeType, MimeType metadataMimeType,
			RSocketStrategies strategies) {

		return new DefaultRSocketRequester(rsocket, dataMimeType, metadataMimeType, strategies);
	}


	/**
	 * Builder to prepare an {@link RSocketRequester} by connecting to an
	 * RSocket server and wrapping the resulting {@link RSocket}.
	 */
	interface Builder {

		/**
		 * Configure the payload data MimeType to specify on the {@code SETUP}
		 * frame that applies to the whole connection.
		 * <p>If this is not set, the builder will try to select the mime type
		 * based on the presence of a single
		 * {@link RSocketStrategies.Builder#decoder(Decoder[])  non-default}
		 * {@code Decoder}, or the first default decoder otherwise
		 * (i.e. {@code String}) if no others are configured.
		 */
		RSocketRequester.Builder dataMimeType(@Nullable MimeType mimeType);

		/**
		 * Configure the payload metadata MimeType to specify on the {@code SETUP}
		 * frame and applies to the whole connection.
		 * <p>By default this is set to
		 * {@code "message/x.rsocket.composite-metadata.v0"} in which case the
		 * route, if provided, is encoded as a
		 * {@code "message/x.rsocket.routing.v0"} composite metadata entry.
		 * For any other MimeType, it is assumed to be the MimeType for the
		 * route, if provided.
		 */
		RSocketRequester.Builder metadataMimeType(MimeType mimeType);

		/**
		 * Set the {@link RSocketStrategies} to use.
		 * <p>By default this is set to {@code RSocketStrategies.builder().build()}
		 * but may be further customized via {@link #rsocketStrategies(Consumer)}.
		 */
		RSocketRequester.Builder rsocketStrategies(@Nullable RSocketStrategies strategies);

		/**
		 * Customize the {@link RSocketStrategies}.
		 * <p>By default this starts out with an empty builder, i.e.
		 * {@link RSocketStrategies#builder()}, but the strategies can also be
		 * set via {@link #rsocketStrategies(RSocketStrategies)}.
		 */
		RSocketRequester.Builder rsocketStrategies(Consumer<RSocketStrategies.Builder> configurer);

		/**
		 * Callback to configure the {@code ClientRSocketFactory} directly.
		 * <p>See {@link AnnotationClientResponderConfigurer} for configuring a
		 * client side responder.
		 * <p><strong>Note:</strong> Do not set {@link #dataMimeType(MimeType)}
		 * and {@link #metadataMimeType(MimeType)} directly on the
		 * {@code ClientRSocketFactory}. Use the shortcuts on this builder
		 * instead since the created {@code RSocketRequester} needs to be aware
		 * of those settings.
		 * @see AnnotationClientResponderConfigurer
		 */
		RSocketRequester.Builder rsocketFactory(ClientRSocketFactoryConfigurer configurer);

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
		 * Use this to append additional metadata entries if the RSocket
		 * connection is configured to use composite metadata. If not, an
		 * {@link IllegalArgumentException} will be raised.
		 * @param metadata an Object, to be encoded with a suitable
		 * {@link org.springframework.core.codec.Encoder Encoder}, or a
		 * {@link org.springframework.core.io.buffer.DataBuffer DataBuffer}
		 * @param mimeType the mime type that describes the metadata
		 */
		RequestSpec metadata(Object metadata, MimeType mimeType);

		/**
		 * Provide payload data. The data can be one of the following:
		 * <ul>
		 * <li>Concrete value
		 * <li>{@link Publisher} of value(s)
		 * <li>Any other producer of value(s) that can be adapted to a
		 * {@link Publisher} via {@link ReactiveAdapterRegistry}
		 * </ul>
		 * @param data the Object to use for payload data
		 * @return spec for declaring the expected response
		 */
		ResponseSpec data(Object data);

		/**
		 * Alternative of {@link #data(Object)} that accepts not only a producer
		 * of value(s) but also a hint for the types of values that will be
		 * produced. The class hint is used to find a compatible {@code Encoder}
		 * once, up front, and used for all values.
		 * @param producer the source of payload data value(s). This must be a
		 * {@link Publisher} or another producer adaptable to a
		 * {@code Publisher} via {@link ReactiveAdapterRegistry}
		 * @param elementClass the type of values to be produced
		 * @return spec for declaring the expected response
		 */
		ResponseSpec data(Object producer, Class<?> elementClass);

		/**
		 * Alternative of {@link #data(Object, Class)} but with a
		 * {@link ParameterizedTypeReference} hint which can provide generic
		 * type information.
		 * @param producer the source of payload data value(s). This must be a
		 * {@link Publisher} or another producer adaptable to a
		 * {@code Publisher} via {@link ReactiveAdapterRegistry}
		 * @param elementTypeRef the type of values to be produced
		 */
		ResponseSpec data(Object producer, ParameterizedTypeReference<?> elementTypeRef);
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
		 * the request payload is a multi-valued {@link Publisher} as
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
