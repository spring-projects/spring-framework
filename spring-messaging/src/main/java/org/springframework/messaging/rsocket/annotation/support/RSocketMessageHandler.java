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

package org.springframework.messaging.rsocket.annotation.support;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.frame.FrameType;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.reactive.MessageMappingMessageHandler;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodReturnValueHandler;
import org.springframework.messaging.rsocket.ClientRSocketFactoryConfigurer;
import org.springframework.messaging.rsocket.DefaultMetadataExtractor;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.RouteMatcher;
import org.springframework.util.StringUtils;

/**
 * Extension of {@link MessageMappingMessageHandler} to use as an RSocket
 * responder by handling incoming streams via {@code @MessageMapping} annotated
 * methods.
 * <p>Use {@link #clientResponder()} and {@link #serverResponder()} to obtain
 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(Function) client} or
 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor) server}
 * side adapters.
 *
 * @author Rossen Stoyanchev
 * @since 5.2
 */
public class RSocketMessageHandler extends MessageMappingMessageHandler {

	private final List<Encoder<?>> encoders = new ArrayList<>();

	@Nullable
	private RSocketStrategies rsocketStrategies;

	@Nullable
	private MetadataExtractor metadataExtractor;

	@Nullable
	private MimeType defaultDataMimeType;

	private MimeType defaultMetadataMimeType = MetadataExtractor.COMPOSITE_METADATA;


	/**
	 * {@inheritDoc}
	 * <p>If {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * is also set, this property is re-initialized with the decoders in it.
	 * Or vice versa, if {@link #setRSocketStrategies(RSocketStrategies)
	 * rsocketStrategies} is not set, it will be initialized from this and
	 * other properties.
	 */
	@Override
	public void setDecoders(List<? extends Decoder<?>> decoders) {
		super.setDecoders(decoders);
	}

	/**
	 * Configure the encoders to use for encoding handler method return values.
	 * <p>If {@link #setRSocketStrategies(RSocketStrategies) rsocketStrategies}
	 * is also set, this property is re-initialized with the encoders in it.
	 * Or vice versa, if {@link #setRSocketStrategies(RSocketStrategies)
	 * rsocketStrategies} is not set, it will be initialized from this and
	 * other properties.
	 */
	public void setEncoders(List<? extends Encoder<?>> encoders) {
		this.encoders.addAll(encoders);
	}

	/**
	 * Return the configured {@link #setEncoders(List) encoders}.
	 */
	public List<? extends Encoder<?>> getEncoders() {
		return this.encoders;
	}

	/**
	 * Provide configuration in the form of {@link RSocketStrategies} instance
	 * which can also be re-used to initialize a client-side
	 * {@link RSocketRequester}.
	 * <p>When this is set, in turn it sets the following:
	 * <ul>
	 * <li>{@link #setDecoders(List)}
	 * <li>{@link #setEncoders(List)}
	 * <li>{@link #setRouteMatcher(RouteMatcher)}
	 * <li>{@link #setMetadataExtractor(MetadataExtractor)}
	 * <li>{@link #setReactiveAdapterRegistry(ReactiveAdapterRegistry)}
	 * </ul>
	 * <p>By default if this is not set, it is initialized from the above.
	 */
	public void setRSocketStrategies(@Nullable RSocketStrategies rsocketStrategies) {
		this.rsocketStrategies = rsocketStrategies;
		if (rsocketStrategies != null) {
			setDecoders(rsocketStrategies.decoders());
			setEncoders(rsocketStrategies.encoders());
			setReactiveAdapterRegistry(rsocketStrategies.reactiveAdapterRegistry());
		}
	}

	/**
	 * Return the configured {@link RSocketStrategies}. This may be {@code null}
	 * before {@link #afterPropertiesSet()} is called.
	 */
	@Nullable
	public RSocketStrategies getRSocketStrategies() {
		return this.rsocketStrategies;
	}

	/**
	 * Configure a {@link MetadataExtractor} to extract the route along with
	 * other metadata.
	 * <p>By default this is {@link DefaultMetadataExtractor} extracting a
	 * route from {@code "message/x.rsocket.routing.v0"} or {@code "text/plain"}.
	 * @param extractor the extractor to use
	 */
	public void setMetadataExtractor(MetadataExtractor extractor) {
		this.metadataExtractor = extractor;
	}

	/**
	 * Return the configured {@link #setMetadataExtractor MetadataExtractor}.
	 * This may be {@code null} before {@link #afterPropertiesSet()}.
	 */
	@Nullable
	public MetadataExtractor getMetadataExtractor() {
		return this.metadataExtractor;
	}

	/**
	 * Configure the default content type to use for data payloads if the
	 * {@code SETUP} frame did not specify one.
	 * <p>By default this is not set.
	 * @param mimeType the MimeType to use
	 */
	public void setDefaultDataMimeType(@Nullable MimeType mimeType) {
		this.defaultDataMimeType = mimeType;
	}

	/**
	 * Return the configured
	 * {@link #setDefaultDataMimeType defaultDataMimeType}, or {@code null}.
	 */
	@Nullable
	public MimeType getDefaultDataMimeType() {
		return this.defaultDataMimeType;
	}

	/**
	 * Configure the default {@code MimeType} for payload data if the
	 * {@code SETUP} frame did not specify one.
	 * <p>By default this is set to {@code "message/x.rsocket.composite-metadata.v0"}
	 * @param mimeType the MimeType to use
	 */
	public void setDefaultMetadataMimeType(MimeType mimeType) {
		Assert.notNull(mimeType, "'metadataMimeType' is required");
		this.defaultMetadataMimeType = mimeType;
	}

	/**
	 * Return the configured
	 * {@link #setDefaultMetadataMimeType defaultMetadataMimeType}.
	 */
	public MimeType getDefaultMetadataMimeType() {
		return this.defaultMetadataMimeType;
	}


	@Override
	public void afterPropertiesSet() {

		getArgumentResolverConfigurer().addCustomResolver(new RSocketRequesterMethodArgumentResolver());
		super.afterPropertiesSet();

		if (getMetadataExtractor() == null) {
			DefaultMetadataExtractor extractor = new DefaultMetadataExtractor();
			extractor.metadataToExtract(MimeTypeUtils.TEXT_PLAIN, String.class, MetadataExtractor.ROUTE_KEY);
			setMetadataExtractor(extractor);
		}

		if (this.rsocketStrategies == null) {
			this.rsocketStrategies = RSocketStrategies.builder()
					.decoder(getDecoders().toArray(new Decoder<?>[0]))
					.encoder(getEncoders().toArray(new Encoder<?>[0]))
					.routeMatcher(getRouteMatcher())
					.metadataExtractor(getMetadataExtractor())
					.reactiveAdapterStrategy(getReactiveAdapterRegistry())
					.build();
		}
	}

	@Override
	protected List<? extends HandlerMethodReturnValueHandler> initReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> handlers = new ArrayList<>();
		handlers.add(new RSocketPayloadReturnValueHandler(this.encoders, getReactiveAdapterRegistry()));
		handlers.addAll(getReturnValueHandlerConfigurer().getCustomHandlers());
		return handlers;
	}


	@Override
	@Nullable
	protected CompositeMessageCondition getCondition(AnnotatedElement element) {
		MessageMapping annot1 = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
		if (annot1 != null && annot1.value().length > 0) {
			String[] patterns = processDestinations(annot1.value());
			return new CompositeMessageCondition(
					RSocketFrameTypeMessageCondition.REQUEST_CONDITION,
					new DestinationPatternsMessageCondition(patterns, getRouteMatcher()));
		}
		ConnectMapping annot2 = AnnotatedElementUtils.findMergedAnnotation(element, ConnectMapping.class);
		if (annot2 != null) {
			String[] patterns = processDestinations(annot2.value());
			return new CompositeMessageCondition(
					RSocketFrameTypeMessageCondition.CONNECT_CONDITION,
					new DestinationPatternsMessageCondition(patterns, getRouteMatcher()));
		}
		return null;
	}


	@Override
	protected void handleNoMatch(@Nullable RouteMatcher.Route destination, Message<?> message) {
		FrameType frameType = RSocketFrameTypeMessageCondition.getFrameType(message);
		if (frameType == FrameType.SETUP || frameType == FrameType.METADATA_PUSH) {
			return;  // optional handling
		}
		if (frameType == FrameType.REQUEST_FNF) {
			// Can't propagate error to client, so just log
			logger.warn("No handler for fireAndForget to '" + destination + "'");
			return;
		}
		throw new MessageDeliveryException("No handler for destination '" + destination + "'");
	}

	/**
	 * Return an adapter for a server side
	 * {@link io.rsocket.RSocketFactory.ServerRSocketFactory#acceptor(SocketAcceptor)
	 * acceptor} that delegate to this {@link RSocketMessageHandler} for
	 * handling.
	 * <p>The initial {@link ConnectionSetupPayload} can be handled with a
	 * {@link ConnectMapping @ConnectionMapping} method which can be asynchronous
	 * and return {@code Mono<Void>} with an error signal preventing the
	 * connection. Such a method can also start requests to the client but that
	 * must be done decoupled from handling and from the current thread.
	 * <p>Subsequent stream requests can be handled with
	 * {@link MessageMapping MessageMapping} methods.
	 */
	public SocketAcceptor serverResponder() {
		return (setupPayload, sendingRSocket) -> {
			MessagingRSocket responder = createResponder(setupPayload, sendingRSocket);
			return responder.handleConnectionSetupPayload(setupPayload).then(Mono.just(responder));
		};
	}

	/**
	 * Return an adapter for a client side
	 * {@link io.rsocket.RSocketFactory.ClientRSocketFactory#acceptor(BiFunction)
	 * acceptor} that delegate to this {@link RSocketMessageHandler} for
	 * handling.
	 * <p>The initial {@link ConnectionSetupPayload} can be processed with a
	 * {@link ConnectMapping @ConnectionMapping} method but, unlike the
	 * server side, such a method is merely a callback and cannot prevent the
	 * connection unless the method throws an error immediately. Such a method
	 * can also start requests to the server but must do so decoupled from
	 * handling and from the current thread.
	 * <p>Subsequent stream requests can be handled with
	 * {@link MessageMapping MessageMapping} methods.
	 */
	public BiFunction<ConnectionSetupPayload, RSocket, RSocket> clientResponder() {
		return (setupPayload, sendingRSocket) -> {
			MessagingRSocket responder = createResponder(setupPayload, sendingRSocket);
			responder.handleConnectionSetupPayload(setupPayload).subscribe();
			return responder;
		};
	}

	private MessagingRSocket createResponder(ConnectionSetupPayload setupPayload, RSocket rsocket) {
		String s = setupPayload.dataMimeType();
		MimeType dataMimeType = StringUtils.hasText(s) ? MimeTypeUtils.parseMimeType(s) : this.defaultDataMimeType;
		Assert.notNull(dataMimeType, "No `dataMimeType` in ConnectionSetupPayload and no default value");

		s = setupPayload.metadataMimeType();
		MimeType metadataMimeType = StringUtils.hasText(s) ? MimeTypeUtils.parseMimeType(s) : this.defaultMetadataMimeType;
		Assert.notNull(metadataMimeType, "No `metadataMimeType` in ConnectionSetupPayload and no default value");

		RSocketStrategies strategies = this.rsocketStrategies;
		Assert.notNull(strategies, "No RSocketStrategies. Was afterPropertiesSet not called?");
		RSocketRequester requester = RSocketRequester.wrap(rsocket, dataMimeType, metadataMimeType, strategies);

		Assert.state(this.metadataExtractor != null,
				() -> "No MetadataExtractor. Was afterPropertiesSet not called?");

		Assert.state(getRouteMatcher() != null,
				() -> "No RouteMatcher. Was afterPropertiesSet not called?");

		return new MessagingRSocket(dataMimeType, metadataMimeType, this.metadataExtractor, requester,
				this, getRouteMatcher(), strategies);
	}


	public static ClientRSocketFactoryConfigurer clientResponder(Object... handlers) {
		return new ResponderConfigurer(handlers);
	}


	private static final class ResponderConfigurer implements ClientRSocketFactoryConfigurer {

		private final List<Object> handlers = new ArrayList<>();

		@Nullable
		private RSocketStrategies strategies;


		private ResponderConfigurer(Object... handlers) {
			Assert.notEmpty(handlers, "No handlers");
			for (Object obj : handlers) {
				this.handlers.add(obj instanceof Class ? BeanUtils.instantiateClass((Class<?>) obj) : obj);
			}
		}

		@Override
		public void configureWithStrategies(RSocketStrategies strategies) {
			this.strategies = strategies;
		}

		@Override
		public void configure(RSocketFactory.ClientRSocketFactory factory) {
			RSocketMessageHandler handler = new RSocketMessageHandler();
			handler.setHandlers(this.handlers);
			handler.setRSocketStrategies(this.strategies);
			handler.afterPropertiesSet();
			factory.acceptor(handler.clientResponder());
		}
	}

}
