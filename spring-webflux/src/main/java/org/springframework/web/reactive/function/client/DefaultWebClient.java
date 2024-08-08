/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.util.context.Context;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * The default implementation of {@link WebClient},
 * as created by the static factory methods.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @since 5.0
 * @see WebClient#create()
 * @see WebClient#create(String)
 */
final class DefaultWebClient implements WebClient {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private static final Mono<ClientResponse> NO_HTTP_CLIENT_RESPONSE_ERROR = Mono.error(
			() -> new IllegalStateException("The underlying HTTP client completed without emitting a response."));

	private static final DefaultClientRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION =
			new DefaultClientRequestObservationConvention();


	private final ExchangeFunction exchangeFunction;

	@Nullable
	private final ExchangeFilterFunction filterFunctions;

	private final UriBuilderFactory uriBuilderFactory;

	@Nullable
	private final HttpHeaders defaultHeaders;

	@Nullable
	private final MultiValueMap<String, String> defaultCookies;

	@Nullable
	private final Consumer<RequestHeadersSpec<?>> defaultRequest;

	private final List<DefaultResponseSpec.StatusHandler> defaultStatusHandlers;

	private final ObservationRegistry observationRegistry;

	@Nullable
	private final ClientRequestObservationConvention observationConvention;

	private final DefaultWebClientBuilder builder;


	DefaultWebClient(ExchangeFunction exchangeFunction, @Nullable ExchangeFilterFunction filterFunctions,
			UriBuilderFactory uriBuilderFactory, @Nullable HttpHeaders defaultHeaders,
			@Nullable MultiValueMap<String, String> defaultCookies, @Nullable Consumer<RequestHeadersSpec<?>> defaultRequest,
			@Nullable Map<Predicate<HttpStatusCode>, Function<ClientResponse, Mono<? extends Throwable>>> statusHandlerMap,
			ObservationRegistry observationRegistry, @Nullable ClientRequestObservationConvention observationConvention,
			DefaultWebClientBuilder builder) {

		this.exchangeFunction = exchangeFunction;
		this.filterFunctions = filterFunctions;
		this.uriBuilderFactory = uriBuilderFactory;
		this.defaultHeaders = defaultHeaders;
		this.defaultCookies = defaultCookies;
		this.defaultRequest = defaultRequest;
		this.defaultStatusHandlers = initStatusHandlers(statusHandlerMap);
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
		this.builder = builder;
	}

	private static List<DefaultResponseSpec.StatusHandler> initStatusHandlers(
			@Nullable Map<Predicate<HttpStatusCode>, Function<ClientResponse, Mono<? extends Throwable>>> handlerMap) {

		return (CollectionUtils.isEmpty(handlerMap) ? Collections.emptyList() :
				handlerMap.entrySet().stream()
						.map(entry -> new DefaultResponseSpec.StatusHandler(entry.getKey(), entry.getValue()))
						.toList());
	}


	@Override
	public RequestHeadersUriSpec<?> get() {
		return methodInternal(HttpMethod.GET);
	}

	@Override
	public RequestHeadersUriSpec<?> head() {
		return methodInternal(HttpMethod.HEAD);
	}

	@Override
	public RequestBodyUriSpec post() {
		return methodInternal(HttpMethod.POST);
	}

	@Override
	public RequestBodyUriSpec put() {
		return methodInternal(HttpMethod.PUT);
	}

	@Override
	public RequestBodyUriSpec patch() {
		return methodInternal(HttpMethod.PATCH);
	}

	@Override
	public RequestHeadersUriSpec<?> delete() {
		return methodInternal(HttpMethod.DELETE);
	}

	@Override
	public RequestHeadersUriSpec<?> options() {
		return methodInternal(HttpMethod.OPTIONS);
	}

	@Override
	public RequestBodyUriSpec method(HttpMethod httpMethod) {
		return methodInternal(httpMethod);
	}

	private RequestBodyUriSpec methodInternal(HttpMethod httpMethod) {
		DefaultRequestBodyUriSpec spec = new DefaultRequestBodyUriSpec(httpMethod);
		if (this.defaultRequest != null) {
			this.defaultRequest.accept(spec);
		}
		return spec;
	}

	@Override
	public Builder mutate() {
		return new DefaultWebClientBuilder(this.builder);
	}

	private static Mono<Void> releaseIfNotConsumed(ClientResponse response) {
		return response.releaseBody().onErrorComplete();
	}

	private static <T> Mono<T> releaseIfNotConsumed(ClientResponse response, Throwable ex) {
		return response.releaseBody().onErrorComplete().then(Mono.error(ex));
	}


	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private final HttpMethod httpMethod;

		@Nullable
		private URI uri;

		@Nullable
		private HttpHeaders headers;

		@Nullable
		private MultiValueMap<String, String> cookies;

		@Nullable
		private BodyInserter<?, ? super ClientHttpRequest> inserter;

		private final Map<String, Object> attributes = new LinkedHashMap<>(4);

		@Nullable
		private Function<Context, Context> contextModifier;

		@Nullable
		private Consumer<ClientHttpRequest> httpRequestConsumer;

		DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
			attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
			return uri(uriBuilder.build(uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
			attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
			return uri(uriBuilder.build(uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Function<UriBuilder, URI> uriFunction) {
			UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
			attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
			return uri(uriFunction.apply(uriBuilder));
		}

		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			return uri(uriFunction.apply(uriBuilderFactory.builder()));
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			this.uri = uri;
			return this;
		}

		private HttpHeaders getHeaders() {
			if (this.headers == null) {
				this.headers = new HttpHeaders();
			}
			return this.headers;
		}

		private MultiValueMap<String, String> getCookies() {
			if (this.cookies == null) {
				this.cookies = new LinkedMultiValueMap<>(3);
			}
			return this.cookies;
		}

		@Override
		public DefaultRequestBodyUriSpec header(String headerName, String... headerValues) {
			for (String headerValue : headerValues) {
				getHeaders().add(headerName, headerValue);
			}
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(getHeaders());
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec accept(MediaType... acceptableMediaTypes) {
			getHeaders().setAccept(Arrays.asList(acceptableMediaTypes));
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec acceptCharset(Charset... acceptableCharsets) {
			getHeaders().setAcceptCharset(Arrays.asList(acceptableCharsets));
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec contentType(MediaType contentType) {
			getHeaders().setContentType(contentType);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec contentLength(long contentLength) {
			getHeaders().setContentLength(contentLength);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec cookie(String name, String value) {
			getCookies().add(name, value);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer) {
			cookiesConsumer.accept(getCookies());
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec ifModifiedSince(ZonedDateTime ifModifiedSince) {
			getHeaders().setIfModifiedSince(ifModifiedSince);
			return this;
		}

		@Override
		public DefaultRequestBodyUriSpec ifNoneMatch(String... ifNoneMatches) {
			getHeaders().setIfNoneMatch(Arrays.asList(ifNoneMatches));
			return this;
		}

		@Override
		public RequestBodySpec attribute(String name, Object value) {
			this.attributes.put(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			attributesConsumer.accept(this.attributes);
			return this;
		}

		@SuppressWarnings("deprecation")
		@Override
		public RequestBodySpec context(Function<Context, Context> contextModifier) {
			this.contextModifier = (this.contextModifier != null ?
					this.contextModifier.andThen(contextModifier) : contextModifier);
			return this;
		}

		@Override
		public RequestBodySpec httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
			this.httpRequestConsumer = (this.httpRequestConsumer != null ?
					this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> bodyValue(Object body) {
			this.inserter = BodyInserters.fromValue(body);
			return this;
		}

		@Override
		public <T> RequestHeadersSpec<?> bodyValue(T body, ParameterizedTypeReference<T> bodyType) {
			this.inserter = BodyInserters.fromValue(body, bodyType);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(
				P publisher, ParameterizedTypeReference<T> elementTypeRef) {
			this.inserter = BodyInserters.fromPublisher(publisher, elementTypeRef);
			return this;
		}

		@Override
		public <T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass) {
			this.inserter = BodyInserters.fromPublisher(publisher, elementClass);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(Object producer, Class<?> elementClass) {
			this.inserter = BodyInserters.fromProducer(producer, elementClass);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef) {
			this.inserter = BodyInserters.fromProducer(producer, elementTypeRef);
			return this;
		}

		@Override
		public RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter) {
			this.inserter = inserter;
			return this;
		}

		@Override
		@Deprecated
		public RequestHeadersSpec<?> syncBody(Object body) {
			return bodyValue(body);
		}

		@Override
		public ResponseSpec retrieve() {
			return new DefaultResponseSpec(
					this.httpMethod, initUri(), exchange(), DefaultWebClient.this.defaultStatusHandlers);
		}

		@Override
		public <V> Mono<V> exchangeToMono(Function<ClientResponse, ? extends Mono<V>> responseHandler) {
			return exchange().flatMap(response -> {
				try {
					return responseHandler.apply(response)
							.flatMap(value -> releaseIfNotConsumed(response).thenReturn(value))
							.switchIfEmpty(Mono.defer(() -> releaseIfNotConsumed(response).then(Mono.empty())))
							.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
				}
				catch (Throwable ex) {
					return releaseIfNotConsumed(response, ex);
				}
			});
		}

		@Override
		public <V> Flux<V> exchangeToFlux(Function<ClientResponse, ? extends Flux<V>> responseHandler) {
			return exchange().flatMapMany(response -> {
				try {
					return responseHandler.apply(response)
							.concatWith(Flux.defer(() -> releaseIfNotConsumed(response).then(Mono.empty())))
							.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
				}
				catch (Throwable ex) {
					return releaseIfNotConsumed(response, ex);
				}
			});
		}

		@SuppressWarnings("deprecation")
		@Override
		public Mono<ClientResponse> exchange() {
			ClientRequest.Builder requestBuilder = initRequestBuilder();
			ClientRequestObservationContext observationContext = new ClientRequestObservationContext(requestBuilder);
			return Mono.deferContextual(contextView -> {
				Observation observation = ClientHttpObservationDocumentation.HTTP_REACTIVE_CLIENT_EXCHANGES.observation(observationConvention,
						DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, observationRegistry);
				observation
						.parentObservation(contextView.getOrDefault(ObservationThreadLocalAccessor.KEY, null))
						.start();
				ExchangeFilterFunction filterFunction = new ObservationFilterFunction(observationContext);
				if (filterFunctions != null) {
					filterFunction = filterFunctions.andThen(filterFunction);
				}
				ClientRequest request = requestBuilder.build();
				observationContext.setUriTemplate((String) request.attribute(URI_TEMPLATE_ATTRIBUTE).orElse(null));
				observationContext.setRequest(request);
				Mono<ClientResponse> responseMono = filterFunction.apply(exchangeFunction)
						.exchange(request)
						.checkpoint("Request to " +
								WebClientUtils.getRequestDescription(request.method(), request.url()) +
								" [DefaultWebClient]")
						.switchIfEmpty(NO_HTTP_CLIENT_RESPONSE_ERROR);
				if (this.contextModifier != null) {
					responseMono = responseMono.contextWrite(this.contextModifier);
				}
				final AtomicBoolean responseReceived = new AtomicBoolean();
				return responseMono
						.doOnNext(response -> responseReceived.set(true))
						.doOnError(observation::error)
						.doFinally(signalType -> {
							if (signalType == SignalType.CANCEL && !responseReceived.get()) {
								observationContext.setAborted(true);
							}
							observation.stop();
						})
						.contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
			});
		}

		private ClientRequest.Builder initRequestBuilder() {
			ClientRequest.Builder builder = ClientRequest.create(this.httpMethod, initUri())
					.headers(this::initHeaders)
					.cookies(this::initCookies)
					.attributes(attributes -> attributes.putAll(this.attributes));
			if (this.httpRequestConsumer != null) {
				builder.httpRequest(this.httpRequestConsumer);
			}
			if (this.inserter != null) {
				builder.body(this.inserter);
			}
			return builder;
		}

		private URI initUri() {
			return (this.uri != null ? this.uri : uriBuilderFactory.expand(""));
		}

		private void initHeaders(HttpHeaders out) {
			if (!CollectionUtils.isEmpty(defaultHeaders)) {
				out.putAll(defaultHeaders);
			}
			if (!CollectionUtils.isEmpty(this.headers)) {
				out.putAll(this.headers);
			}
		}

		private void initCookies(MultiValueMap<String, String> out) {
			if (!CollectionUtils.isEmpty(defaultCookies)) {
				out.putAll(defaultCookies);
			}
			if (!CollectionUtils.isEmpty(this.cookies)) {
				out.putAll(this.cookies);
			}
		}
	}


	private static class DefaultResponseSpec implements ResponseSpec {

		private static final StatusHandler DEFAULT_STATUS_HANDLER =
				new StatusHandler(code -> code.value() >= 400, ClientResponse::createException);


		private final HttpMethod httpMethod;

		private final URI uri;

		private final Mono<ClientResponse> responseMono;

		private final List<StatusHandler> statusHandlers = new ArrayList<>(1);

		private final int defaultStatusHandlerCount;

		DefaultResponseSpec(HttpMethod httpMethod, URI uri, Mono<ClientResponse> responseMono,
				List<StatusHandler> defaultStatusHandlers) {

			this.httpMethod = httpMethod;
			this.uri = uri;
			this.responseMono = responseMono;
			this.statusHandlers.addAll(defaultStatusHandlers);
			this.statusHandlers.add(DEFAULT_STATUS_HANDLER);
			this.defaultStatusHandlerCount = this.statusHandlers.size();
		}

		@Override
		public ResponseSpec onStatus(Predicate<HttpStatusCode> statusCodePredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

			Assert.notNull(statusCodePredicate, "StatusCodePredicate must not be null");
			Assert.notNull(exceptionFunction, "Function must not be null");
			int index = this.statusHandlers.size() - this.defaultStatusHandlerCount;  // Default handlers always last
			this.statusHandlers.add(index, new StatusHandler(statusCodePredicate, exceptionFunction));
			return this;
		}

		@Override
		public ResponseSpec onRawStatus(IntPredicate statusCodePredicate,
				Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

			return onStatus(toStatusCodePredicate(statusCodePredicate), exceptionFunction);
		}

		private static Predicate<HttpStatusCode> toStatusCodePredicate(IntPredicate predicate) {
			return value -> predicate.test(value.value());
		}

		@Override
		public <T> Mono<T> bodyToMono(Class<T> elementClass) {
			Assert.notNull(elementClass, "Class must not be null");
			return this.responseMono.flatMap(response ->
					handleBodyMono(response, response.bodyToMono(elementClass)));
		}

		@Override
		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
			Assert.notNull(elementTypeRef, "ParameterizedTypeReference must not be null");
			return this.responseMono.flatMap(response ->
					handleBodyMono(response, response.bodyToMono(elementTypeRef)));
		}

		@Override
		public <T> Flux<T> bodyToFlux(Class<T> elementClass) {
			Assert.notNull(elementClass, "Class must not be null");
			return this.responseMono.flatMapMany(response ->
					handleBodyFlux(response, response.bodyToFlux(elementClass)));
		}

		@Override
		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
			Assert.notNull(elementTypeRef, "ParameterizedTypeReference must not be null");
			return this.responseMono.flatMapMany(response ->
					handleBodyFlux(response, response.bodyToFlux(elementTypeRef)));
		}

		@Override
		public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntity(response,
							handleBodyMono(response, response.bodyToMono(bodyClass))));
		}

		@Override
		public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeRef) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntity(response,
							handleBodyMono(response, response.bodyToMono(bodyTypeRef))));
		}

		@Override
		public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntityList(response,
							handleBodyFlux(response, response.bodyToFlux(elementClass))));
		}

		@Override
		public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntityList(response,
							handleBodyFlux(response, response.bodyToFlux(elementTypeRef))));
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementType) {
			return this.responseMono.flatMap(response ->
					handlerEntityFlux(response, response.bodyToFlux(elementType)));
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeRef) {
			return this.responseMono.flatMap(response ->
					handlerEntityFlux(response, response.bodyToFlux(elementTypeRef)));
		}

		@Override
		public <T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor) {
			return this.responseMono.flatMap(response ->
					handlerEntityFlux(response, response.body(bodyExtractor)));
		}

		@Override
		public Mono<ResponseEntity<Void>> toBodilessEntity() {
			return this.responseMono.flatMap(response ->
					WebClientUtils.mapToEntity(response, handleBodyMono(response, Mono.<Void>empty()))
							.flatMap(entity -> response.releaseBody()
									.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response))
									.thenReturn(entity))
			);
		}

		private <T> Mono<T> handleBodyMono(ClientResponse response, Mono<T> body) {
			body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));
			Mono<T> result = applyStatusHandlers(response);
			return (result != null ? result.switchIfEmpty(body) : body);
		}

		private <T> Publisher<T> handleBodyFlux(ClientResponse response, Flux<T> body) {
			body = body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response));
			Mono<T> result = applyStatusHandlers(response);
			return (result != null ? result.flux().switchIfEmpty(body) : body);
		}

		private <T> Mono<? extends ResponseEntity<Flux<T>>> handlerEntityFlux(ClientResponse response, Flux<T> body) {
			ResponseEntity<Flux<T>> entity = new ResponseEntity<>(
					body.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, exceptionWrappingFunction(response)),
					response.headers().asHttpHeaders(),
					response.statusCode());

			Mono<ResponseEntity<Flux<T>>> result = applyStatusHandlers(response);
			return (result != null ? result.defaultIfEmpty(entity) : Mono.just(entity));
		}

		private <T> Function<Throwable, Mono<? extends T>> exceptionWrappingFunction(ClientResponse response) {
			return t -> response.createException().flatMap(ex -> Mono.error(ex.initCause(t)));
		}

		@Nullable
		private <T> Mono<T> applyStatusHandlers(ClientResponse response) {
			HttpStatusCode statusCode = response.statusCode();
			for (StatusHandler handler : this.statusHandlers) {
				if (handler.test(statusCode)) {
					Mono<? extends Throwable> exMono;
					try {
						exMono = handler.apply(response);
						exMono = exMono.flatMap(ex -> releaseIfNotConsumed(response, ex));
						exMono = exMono.onErrorResume(ex -> releaseIfNotConsumed(response, ex));
					}
					catch (Throwable ex2) {
						exMono = releaseIfNotConsumed(response, ex2);
					}
					Mono<T> result = exMono.flatMap(Mono::error);
					return result.checkpoint(statusCode + " from " +
							WebClientUtils.getRequestDescription(this.httpMethod, this.uri) +
							" [DefaultWebClient]");
				}
			}
			return null;
		}


		private static class StatusHandler {

			private final Predicate<HttpStatusCode> predicate;

			private final Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction;

			public StatusHandler(Predicate<HttpStatusCode> predicate,
					Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction) {

				this.predicate = predicate;
				this.exceptionFunction = exceptionFunction;
			}

			public boolean test(HttpStatusCode status) {
				return this.predicate.test(status);
			}

			public Mono<? extends Throwable> apply(ClientResponse response) {
				return this.exceptionFunction.apply(response);
			}
		}
	}


	private static class ObservationFilterFunction implements ExchangeFilterFunction {

		private final ClientRequestObservationContext observationContext;

		ObservationFilterFunction(ClientRequestObservationContext observationContext) {
			this.observationContext = observationContext;
		}

		@Override
		public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
			return next.exchange(request).doOnNext(this.observationContext::setResponse);
		}
	}

}
