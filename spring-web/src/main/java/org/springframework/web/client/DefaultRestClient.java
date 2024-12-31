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

package org.springframework.web.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.observation.ClientHttpObservationDocumentation;
import org.springframework.http.client.observation.ClientRequestObservationContext;
import org.springframework.http.client.observation.ClientRequestObservationConvention;
import org.springframework.http.client.observation.DefaultClientRequestObservationConvention;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.SmartHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;

/**
 * The default implementation of {@link RestClient},
 * as created by the static factory methods.
 *
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @since 6.1
 * @see RestClient#create()
 * @see RestClient#create(String)
 * @see RestClient#create(RestTemplate)
 */
final class DefaultRestClient implements RestClient {

	private static final Log logger = LogFactory.getLog(DefaultRestClient.class);

	private static final ClientRequestObservationConvention DEFAULT_OBSERVATION_CONVENTION = new DefaultClientRequestObservationConvention();

	private static final String URI_TEMPLATE_ATTRIBUTE = RestClient.class.getName() + ".uriTemplate";


	private final ClientHttpRequestFactory clientRequestFactory;

	private volatile @Nullable ClientHttpRequestFactory interceptingRequestFactory;

	private final @Nullable List<ClientHttpRequestInitializer> initializers;

	private final @Nullable List<ClientHttpRequestInterceptor> interceptors;

	private final UriBuilderFactory uriBuilderFactory;

	private final @Nullable HttpHeaders defaultHeaders;

	private final @Nullable MultiValueMap<String, String> defaultCookies;

	private final @Nullable Consumer<RequestHeadersSpec<?>> defaultRequest;

	private final List<StatusHandler> defaultStatusHandlers;

	private final DefaultRestClientBuilder builder;

	private final List<HttpMessageConverter<?>> messageConverters;

	private final ObservationRegistry observationRegistry;

	private final @Nullable ClientRequestObservationConvention observationConvention;


	DefaultRestClient(ClientHttpRequestFactory clientRequestFactory,
			@Nullable List<ClientHttpRequestInterceptor> interceptors,
			@Nullable List<ClientHttpRequestInitializer> initializers,
			UriBuilderFactory uriBuilderFactory,
			@Nullable HttpHeaders defaultHeaders,
			@Nullable MultiValueMap<String, String> defaultCookies,
			@Nullable Consumer<RequestHeadersSpec<?>> defaultRequest,
			@Nullable List<StatusHandler> statusHandlers,
			List<HttpMessageConverter<?>> messageConverters,
			ObservationRegistry observationRegistry,
			@Nullable ClientRequestObservationConvention observationConvention,
			DefaultRestClientBuilder builder) {

		this.clientRequestFactory = clientRequestFactory;
		this.initializers = initializers;
		this.interceptors = interceptors;
		this.uriBuilderFactory = uriBuilderFactory;
		this.defaultHeaders = defaultHeaders;
		this.defaultCookies = defaultCookies;
		this.defaultRequest = defaultRequest;
		this.defaultStatusHandlers = (statusHandlers != null ? new ArrayList<>(statusHandlers) : new ArrayList<>());
		this.messageConverters = messageConverters;
		this.observationRegistry = observationRegistry;
		this.observationConvention = observationConvention;
		this.builder = builder;
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
	public RequestBodyUriSpec method(HttpMethod method) {
		Assert.notNull(method, "HttpMethod must not be null");
		return methodInternal(method);
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
		return new DefaultRestClientBuilder(this.builder);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private <T> @Nullable T readWithMessageConverters(
			ClientHttpResponse clientResponse, Runnable callback, Type bodyType, Class<T> bodyClass) {

		MediaType contentType = getContentType(clientResponse);

		try {
			callback.run();

			IntrospectingClientHttpResponse responseWrapper = new IntrospectingClientHttpResponse(clientResponse);
			if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
				return null;
			}

			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
					if (genericMessageConverter.canRead(bodyType, null, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Reading to [" + ResolvableType.forType(bodyType) + "]");
						}
						return (T) genericMessageConverter.read(bodyType, null, responseWrapper);
					}
				}
				else if (messageConverter instanceof SmartHttpMessageConverter smartMessageConverter) {
					ResolvableType resolvableType = ResolvableType.forType(bodyType);
					if (smartMessageConverter.canRead(resolvableType, contentType)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Reading to [" + resolvableType + "]");
						}
						return (T) smartMessageConverter.read(resolvableType, responseWrapper, null);
					}
				}
				else if (messageConverter.canRead(bodyClass, contentType)) {
					if (logger.isDebugEnabled()) {
						logger.debug("Reading to [" + bodyClass.getName() + "] as \"" + contentType + "\"");
					}
					return (T) messageConverter.read((Class)bodyClass, responseWrapper);
				}
			}

			throw new UnknownContentTypeException(bodyType, contentType,
					responseWrapper.getStatusCode(), responseWrapper.getStatusText(),
					responseWrapper.getHeaders(), RestClientUtils.getBody(responseWrapper));
		}
		catch (UncheckedIOException | IOException | HttpMessageNotReadableException exc) {
			Throwable cause;
			if (exc instanceof UncheckedIOException uncheckedIOException) {
				cause = uncheckedIOException.getCause();
			}
			else {
				cause = exc;
			}
			throw new RestClientException("Error while extracting response for type [" +
					ResolvableType.forType(bodyType) + "] and content type [" + contentType + "]", cause);
		}
	}

	private static MediaType getContentType(ClientHttpResponse clientResponse) {
		MediaType contentType = clientResponse.getHeaders().getContentType();
		if (contentType == null) {
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		return contentType;
	}

	@SuppressWarnings("unchecked")
	private static <T> Class<T> bodyClass(Type type) {
		if (type instanceof Class<?> clazz) {
			return (Class<T>) clazz;
		}
		if (type instanceof ParameterizedType parameterizedType &&
				parameterizedType.getRawType() instanceof Class<?> rawType) {
			return (Class<T>) rawType;
		}
		return (Class<T>) Object.class;
	}




	private class DefaultRequestBodyUriSpec implements RequestBodyUriSpec {

		private final HttpMethod httpMethod;

		private @Nullable URI uri;

		private @Nullable HttpHeaders headers;

		private @Nullable MultiValueMap<String, String> cookies;

		private @Nullable InternalBody body;

		private @Nullable Map<String, Object> attributes;

		private @Nullable Consumer<ClientHttpRequest> httpRequestConsumer;

		public DefaultRequestBodyUriSpec(HttpMethod httpMethod) {
			this.httpMethod = httpMethod;
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Object... uriVariables) {
			UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
			attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
			return uri(DefaultRestClient.this.uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Map<String, ?> uriVariables) {
			UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
			attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
			return uri(DefaultRestClient.this.uriBuilderFactory.expand(uriTemplate, uriVariables));
		}

		@Override
		public RequestBodySpec uri(String uriTemplate, Function<UriBuilder, URI> uriFunction) {
			UriBuilder uriBuilder = uriBuilderFactory.uriString(uriTemplate);
			attribute(URI_TEMPLATE_ATTRIBUTE, uriBuilder.toUriString());
			return uri(uriFunction.apply(DefaultRestClient.this.uriBuilderFactory.uriString(uriTemplate)));
		}

		@Override
		public RequestBodySpec uri(Function<UriBuilder, URI> uriFunction) {
			return uri(uriFunction.apply(DefaultRestClient.this.uriBuilderFactory.builder()));
		}

		@Override
		public RequestBodySpec uri(URI uri) {
			if (uri.isAbsolute()) {
				this.uri = uri;
			}
			else {
				URI baseUri = DefaultRestClient.this.uriBuilderFactory.expand("");
				this.uri = baseUri.resolve(uri);
			}
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
			getAttributes().put(name, value);
			return this;
		}

		@Override
		public RequestBodySpec attributes(Consumer<Map<String, Object>> attributesConsumer) {
			attributesConsumer.accept(getAttributes());
			return this;
		}

		private Map<String, Object> getAttributes() {
			Map<String, Object> attributes = this.attributes;
			if (attributes == null) {
				attributes = new ConcurrentHashMap<>(4);
				this.attributes = attributes;
			}
			return attributes;
		}

		@Override
		public RequestBodySpec httpRequest(Consumer<ClientHttpRequest> requestConsumer) {
			this.httpRequestConsumer = (this.httpRequestConsumer != null ?
					this.httpRequestConsumer.andThen(requestConsumer) : requestConsumer);
			return this;
		}

		@Override
		public RequestBodySpec body(Object body) {
			this.body = clientHttpRequest -> writeWithMessageConverters(body, body.getClass(), clientHttpRequest);
			return this;
		}

		@Override
		public <T> RequestBodySpec body(T body, ParameterizedTypeReference<T> bodyType) {
			this.body = clientHttpRequest -> writeWithMessageConverters(body, bodyType.getType(), clientHttpRequest);
			return this;
		}

		@Override
		public RequestBodySpec body(StreamingHttpOutputMessage.Body body) {
			this.body = request -> body.writeTo(request.getBody());
			return this;
		}

		@SuppressWarnings({"rawtypes", "unchecked"})
		private void writeWithMessageConverters(Object body, Type bodyType, ClientHttpRequest clientRequest)
				throws IOException {

			MediaType contentType = clientRequest.getHeaders().getContentType();
			Class<?> bodyClass = body.getClass();

			for (HttpMessageConverter messageConverter : DefaultRestClient.this.messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter genericMessageConverter) {
					if (genericMessageConverter.canWrite(bodyType, bodyClass, contentType)) {
						logBody(body, contentType, genericMessageConverter);
						genericMessageConverter.write(body, bodyType, contentType, clientRequest);
						return;
					}
				}
				else if (messageConverter instanceof SmartHttpMessageConverter smartMessageConverter) {
					ResolvableType resolvableType = ResolvableType.forType(bodyType);
					if (smartMessageConverter.canWrite(resolvableType, bodyClass, contentType)) {
						logBody(body, contentType, smartMessageConverter);
						smartMessageConverter.write(body, resolvableType, contentType, clientRequest, null);
						return;
					}
				}
				else if (messageConverter.canWrite(bodyClass, contentType)) {
					logBody(body, contentType, messageConverter);
					messageConverter.write(body, contentType, clientRequest);
					return;
				}
			}
			String message = "No HttpMessageConverter for " + bodyClass.getName();
			if (contentType != null) {
				message += " and content type \"" + contentType + "\"";
			}
			throw new RestClientException(message);
		}

		private void logBody(Object body, @Nullable MediaType mediaType, HttpMessageConverter<?> converter) {
			if (logger.isDebugEnabled()) {
				StringBuilder msg = new StringBuilder("Writing [");
				msg.append(body);
				msg.append("] ");
				if (mediaType != null) {
					msg.append("as \"");
					msg.append(mediaType);
					msg.append("\" ");
				}
				msg.append("with ");
				msg.append(converter.getClass().getName());
				logger.debug(msg.toString());
			}
		}


		@Override
		public ResponseSpec retrieve() {
			return new DefaultResponseSpec(this);
		}

		@Override
		public <T> @Nullable T exchange(ExchangeFunction<T> exchangeFunction, boolean close) {
			return exchangeInternal(exchangeFunction, close);
		}

		private <T> @Nullable T exchangeInternal(ExchangeFunction<T> exchangeFunction, boolean close) {
			Assert.notNull(exchangeFunction, "ExchangeFunction must not be null");

			ClientHttpResponse clientResponse = null;
			Observation observation = null;
			Observation.Scope observationScope = null;
			URI uri = null;
			try {
				uri = initUri();
				String serializedCookies = serializeCookies();
				if (serializedCookies != null) {
					getHeaders().set(HttpHeaders.COOKIE, serializedCookies);
				}
				HttpHeaders headers = initHeaders();

				ClientHttpRequest clientRequest = createRequest(uri);
				if (headers != null) {
					clientRequest.getHeaders().addAll(headers);
				}
				Map<String, Object> attributes = getAttributes();
				clientRequest.getAttributes().putAll(attributes);
				ClientRequestObservationContext observationContext = new ClientRequestObservationContext(clientRequest);
				observationContext.setUriTemplate((String) attributes.get(URI_TEMPLATE_ATTRIBUTE));
				observation = ClientHttpObservationDocumentation.HTTP_CLIENT_EXCHANGES.observation(observationConvention,
						DEFAULT_OBSERVATION_CONVENTION, () -> observationContext, observationRegistry).start();
				observationScope = observation.openScope();
				if (this.body != null) {
					this.body.writeTo(clientRequest);
				}
				if (this.httpRequestConsumer != null) {
					this.httpRequestConsumer.accept(clientRequest);
				}
				clientResponse = clientRequest.execute();
				observationContext.setResponse(clientResponse);
				ConvertibleClientHttpResponse convertibleWrapper = new DefaultConvertibleClientHttpResponse(clientResponse);
				return exchangeFunction.exchange(clientRequest, convertibleWrapper);
			}
			catch (IOException ex) {
				ResourceAccessException resourceAccessException = createResourceAccessException(uri, this.httpMethod, ex);
				if (observation != null) {
					observation.error(resourceAccessException);
				}
				throw resourceAccessException;
			}
			catch (Throwable error) {
				if (observation != null) {
					observation.error(error);
				}
				throw error;
			}
			finally {
				if (observationScope != null) {
					observationScope.close();
				}
				if (observation != null) {
					observation.stop();
				}
				if (close && clientResponse != null) {
					clientResponse.close();
				}
			}
		}

		private URI initUri() {
			return (this.uri != null ? this.uri : DefaultRestClient.this.uriBuilderFactory.expand(""));
		}

		private @Nullable String serializeCookies() {
			MultiValueMap<String, String> map;
			MultiValueMap<String, String> defaultCookies = DefaultRestClient.this.defaultCookies;
			if (CollectionUtils.isEmpty(this.cookies)) {
				map = defaultCookies;
			}
			else if (CollectionUtils.isEmpty(defaultCookies)) {
				map = this.cookies;
			}
			else {
				map = new LinkedMultiValueMap<>(defaultCookies.size() + this.cookies.size());
				map.putAll(defaultCookies);
				map.putAll(this.cookies);
			}
			return (!CollectionUtils.isEmpty(map) ? serializeCookies(map) : null);
		}

		private static String serializeCookies(MultiValueMap<String, String> map) {
			boolean first = true;
			StringBuilder sb = new StringBuilder();
			for (Map.Entry<String, List<String>> entry : map.entrySet()) {
				for (String value : entry.getValue()) {
					if (!first) {
						sb.append("; ");
					}
					else {
						first = false;
					}
					sb.append(entry.getKey()).append("=").append(value);
				}
			}
			return sb.toString();
		}

		private @Nullable HttpHeaders initHeaders() {
			HttpHeaders defaultHeaders = DefaultRestClient.this.defaultHeaders;
			if (this.headers == null || this.headers.isEmpty()) {
				return defaultHeaders;
			}
			else if (defaultHeaders == null || defaultHeaders.isEmpty()) {
				return this.headers;
			}
			else {
				HttpHeaders result = new HttpHeaders();
				result.putAll(defaultHeaders);
				result.putAll(this.headers);
				return result;
			}
		}

		private ClientHttpRequest createRequest(URI uri) throws IOException {
			ClientHttpRequestFactory factory;
			if (DefaultRestClient.this.interceptors != null) {
				factory = DefaultRestClient.this.interceptingRequestFactory;
				if (factory == null) {
					factory = new InterceptingClientHttpRequestFactory(
							DefaultRestClient.this.clientRequestFactory, DefaultRestClient.this.interceptors);
					DefaultRestClient.this.interceptingRequestFactory = factory;
				}
			}
			else {
				factory = DefaultRestClient.this.clientRequestFactory;
			}
			ClientHttpRequest request = factory.createRequest(uri, this.httpMethod);
			if (DefaultRestClient.this.initializers != null) {
				DefaultRestClient.this.initializers.forEach(initializer -> initializer.initialize(request));
			}
			return request;
		}

		private static ResourceAccessException createResourceAccessException(URI url, HttpMethod method, IOException ex) {
			StringBuilder msg = new StringBuilder("I/O error on ");
			msg.append(method.name());
			msg.append(" request for \"");
			String urlString = url.toString();
			int idx = urlString.indexOf('?');
			if (idx != -1) {
				msg.append(urlString, 0, idx);
			}
			else {
				msg.append(urlString);
			}
			msg.append("\": ");
			msg.append(ex.getMessage());
			return new ResourceAccessException(msg.toString(), ex);
		}


		@FunctionalInterface
		private interface InternalBody {

			void writeTo(ClientHttpRequest request) throws IOException;
		}
	}


	private class DefaultResponseSpec implements ResponseSpec {

		private final RequestHeadersSpec<?> requestHeadersSpec;

		private final List<StatusHandler> statusHandlers = new ArrayList<>(1);

		private final int defaultStatusHandlerCount;

		DefaultResponseSpec(RequestHeadersSpec<?> requestHeadersSpec) {
			this.requestHeadersSpec = requestHeadersSpec;
			this.statusHandlers.addAll(DefaultRestClient.this.defaultStatusHandlers);
			this.statusHandlers.add(StatusHandler.defaultHandler(DefaultRestClient.this.messageConverters));
			this.defaultStatusHandlerCount = this.statusHandlers.size();
		}

		@Override
		public ResponseSpec onStatus(Predicate<HttpStatusCode> statusPredicate, ErrorHandler errorHandler) {
			Assert.notNull(statusPredicate, "StatusPredicate must not be null");
			Assert.notNull(errorHandler, "ErrorHandler must not be null");

			return onStatusInternal(StatusHandler.of(statusPredicate, errorHandler));
		}

		@Override
		public ResponseSpec onStatus(ResponseErrorHandler errorHandler) {
			Assert.notNull(errorHandler, "ResponseErrorHandler must not be null");

			return onStatusInternal(StatusHandler.fromErrorHandler(errorHandler));
		}

		private ResponseSpec onStatusInternal(StatusHandler statusHandler) {
			Assert.notNull(statusHandler, "StatusHandler must not be null");

			int index = this.statusHandlers.size() - this.defaultStatusHandlerCount;  // Default handlers always last
			this.statusHandlers.add(index, statusHandler);
			return this;
		}

		@Override
		public <T> @Nullable T body(Class<T> bodyType) {
			return executeAndExtract((request, response) -> readBody(request, response, bodyType, bodyType));
		}

		@Override
		public <T> @Nullable T body(ParameterizedTypeReference<T> bodyType) {
			Type type = bodyType.getType();
			Class<T> bodyClass = bodyClass(type);
			return executeAndExtract((request, response) -> readBody(request, response, type, bodyClass));
		}

		@Override
		public <T> ResponseEntity<T> toEntity(Class<T> bodyType) {
			return toEntityInternal(bodyType, bodyType);
		}

		@Override
		public <T> ResponseEntity<T> toEntity(ParameterizedTypeReference<T> bodyType) {
			Type type = bodyType.getType();
			Class<T> bodyClass = bodyClass(type);
			return toEntityInternal(type, bodyClass);
		}

		private <T> ResponseEntity<T> toEntityInternal(Type bodyType, Class<T> bodyClass) {
			ResponseEntity<T> entity = executeAndExtract((request, response) -> {
				T body = readBody(request, response, bodyType, bodyClass);
				try {
					return ResponseEntity.status(response.getStatusCode())
							.headers(response.getHeaders())
							.body(body);
				}
				catch (IOException ex) {
					throw new ResourceAccessException(
							"Could not retrieve response status code: " + ex.getMessage(), ex);
				}
			});
			Assert.state(entity != null, "No ResponseEntity");
			return entity;
		}

		@Override
		public ResponseEntity<Void> toBodilessEntity() {
			ResponseEntity<Void> entity = executeAndExtract((request, response) -> {
				try (response) {
					applyStatusHandlers(request, response);
					return ResponseEntity.status(response.getStatusCode())
							.headers(response.getHeaders())
							.build();
				}
				catch (UncheckedIOException ex) {
					throw new ResourceAccessException(
							"Could not retrieve response status code: " + ex.getMessage(), ex.getCause());
				}
				catch (IOException ex) {
					throw new ResourceAccessException(
							"Could not retrieve response status code: " + ex.getMessage(), ex);
				}
			});
			Assert.state(entity != null, "No ResponseEntity");
			return entity;
		}

		public <T> @Nullable T executeAndExtract(RequestHeadersSpec.ExchangeFunction<T> exchangeFunction) {
			return this.requestHeadersSpec.exchange(exchangeFunction);
		}

		private <T> @Nullable T readBody(HttpRequest request, ClientHttpResponse response, Type bodyType, Class<T> bodyClass) {
			return DefaultRestClient.this.readWithMessageConverters(
					response, () -> applyStatusHandlers(request, response), bodyType, bodyClass);

		}

		private void applyStatusHandlers(HttpRequest request, ClientHttpResponse response) {
			try {
				if (response instanceof DefaultConvertibleClientHttpResponse convertibleResponse) {
					response = convertibleResponse.delegate;
				}
				for (StatusHandler handler : this.statusHandlers) {
					if (handler.test(response)) {
						handler.handle(request, response);
						return;
					}
				}
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
		}

	}


	private class DefaultConvertibleClientHttpResponse implements RequestHeadersSpec.ConvertibleClientHttpResponse {

		private final ClientHttpResponse delegate;

		public DefaultConvertibleClientHttpResponse(ClientHttpResponse delegate) {
			this.delegate = delegate;
		}

		@Override
		public <T> @Nullable T bodyTo(Class<T> bodyType) {
			return readWithMessageConverters(this.delegate, () -> {} , bodyType, bodyType);
		}

		@Override
		public <T> @Nullable T bodyTo(ParameterizedTypeReference<T> bodyType) {
			Type type = bodyType.getType();
			Class<T> bodyClass = bodyClass(type);
			return readWithMessageConverters(this.delegate, () -> {}, type, bodyClass);
		}

		@Override
		public InputStream getBody() throws IOException {
			return this.delegate.getBody();
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.delegate.getHeaders();
		}

		@Override
		public HttpStatusCode getStatusCode() throws IOException {
			return this.delegate.getStatusCode();
		}

		@Override
		public String getStatusText() throws IOException {
			return this.delegate.getStatusText();
		}

		@Override
		public void close() {
			this.delegate.close();
		}

	}


}
