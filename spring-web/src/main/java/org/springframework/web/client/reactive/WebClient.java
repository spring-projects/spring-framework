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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

import org.reactivestreams.Publisher;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.ByteBufferDecoder;
import org.springframework.core.codec.ByteBufferEncoder;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.codec.StringEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.codec.xml.Jaxb2Decoder;
import org.springframework.http.codec.xml.Jaxb2Encoder;
import org.springframework.http.converter.reactive.CodecHttpMessageConverter;
import org.springframework.http.converter.reactive.HttpMessageConverter;
import org.springframework.http.converter.reactive.ResourceHttpMessageConverter;
import org.springframework.util.ClassUtils;

import reactor.core.publisher.Mono;

/**
 * Reactive Web client supporting the HTTP/1.1 protocol
 *
 * <p>Here is a simple example of a GET request:
 *
 * <pre class="code">
 * static imports: ClientWebRequestBuilder.*, ResponseExtractors.*
 *
 * // should be shared between HTTP calls
 * WebClient client = new WebClient(new ReactorHttpClient());
 *
 * Mono&lt;String&gt; result = client
 * 		.perform(get("http://example.org/resource").accept(MediaType.TEXT_PLAIN))
 * 		.extract(body(String.class));
 * </pre>
 *
 * <p>This Web client relies on the following:
 * <ul>
 * <li>{@link ClientHttpConnector} implementation to drive the underlying
 * library (e.g. Reactor-Netty)</li>
 * <li>{@link ClientWebRequestBuilder} to create a Web request with a builder
 * API (see {@link ClientWebRequestBuilders})</li>
 * <li>{@link ResponseExtractor} to extract the relevant part of the server
 * response with the composition API of choice (see {@link ResponseExtractors}</li>
 * </ul>
 *
 * @author Brian Clozel
 * @since 5.0
 * @see ClientWebRequestBuilders
 * @see ResponseExtractors
 */
public final class WebClient {

	private static final ClassLoader classLoader = WebClient.class.getClassLoader();

	private static final boolean jackson2Present = ClassUtils
			.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader)
			&& ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",
			classLoader);

	private static final boolean jaxb2Present = ClassUtils
			.isPresent("javax.xml.bind.Binder", classLoader);

	private ClientHttpConnector clientHttpConnector;

	private final DefaultWebClientConfig webClientConfig;

	/**
	 * Create a {@code WebClient} instance, using the {@link ClientHttpConnector}
	 * implementation given as an argument to drive the underlying
	 * implementation.
	 *
	 * Register by default the following Encoders and Decoders:
	 * <ul>
	 * <li>{@link ByteBufferEncoder} / {@link ByteBufferDecoder}</li>
	 * <li>{@link StringEncoder} / {@link StringDecoder}</li>
	 * <li>{@link Jaxb2Encoder} / {@link Jaxb2Decoder}</li>
	 * <li>{@link JacksonJsonEncoder} / {@link JacksonJsonDecoder}</li>
	 * </ul>
	 *
	 * @param clientHttpConnector the {@code ClientHttpRequestFactory} to use
	 */
	public WebClient(ClientHttpConnector clientHttpConnector) {
		this.clientHttpConnector = clientHttpConnector;
		this.webClientConfig = new DefaultWebClientConfig();
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		addDefaultHttpMessageConverters(converters);
		this.webClientConfig.setMessageConverters(converters);
		this.webClientConfig.setResponseErrorHandler(new DefaultResponseErrorHandler());
	}

	/**
	 * Adds default HTTP message converters
	 */
	protected final void addDefaultHttpMessageConverters(
			List<HttpMessageConverter<?>> converters) {
		converters.add(converter(new ByteBufferEncoder(), new ByteBufferDecoder()));
		converters.add(converter(new StringEncoder(), new StringDecoder()));
		converters.add(new ResourceHttpMessageConverter());
		if (jaxb2Present) {
			converters.add(converter(new Jaxb2Encoder(), new Jaxb2Decoder()));
		}
		if (jackson2Present) {
			converters.add(converter(new JacksonJsonEncoder(), new JacksonJsonDecoder()));
		}
	}

	private static <T> HttpMessageConverter<T> converter(Encoder<T> encoder,
			Decoder<T> decoder) {
		return new CodecHttpMessageConverter<>(encoder, decoder);
	}

	/**
	 * Set the list of {@link HttpMessageConverter}s to use for encoding and decoding HTTP
	 * messages
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.webClientConfig.setMessageConverters(messageConverters);
	}

	/**
	 * Set the {@link ResponseErrorHandler} to use for handling HTTP response errors
	 */
	public void setResponseErrorHandler(ResponseErrorHandler responseErrorHandler) {
		this.webClientConfig.setResponseErrorHandler(responseErrorHandler);
	}

	/**
	 * Perform the actual HTTP request/response exchange
	 *
	 * <p>
	 * Requesting from the exposed {@code Flux} will result in:
	 * <ul>
	 * <li>building the actual HTTP request using the provided {@code ClientWebRequestBuilder}</li>
	 * <li>encoding the HTTP request body with the configured {@code HttpMessageConverter}s</li>
	 * <li>returning the response with a publisher of the body</li>
	 * </ul>
	 */
	public WebResponseActions perform(ClientWebRequestBuilder builder) {

		ClientWebRequest clientWebRequest = builder.build();

		final Mono<ClientHttpResponse> clientResponse = this.clientHttpConnector
				.connect(clientWebRequest.getMethod(), clientWebRequest.getUrl(),
						new DefaultRequestCallback(clientWebRequest))
				.log("org.springframework.web.client.reactive", Level.FINE);

		return new WebResponseActions() {
			@Override
			public void doWithStatus(Consumer<HttpStatus> consumer) {
				clientResponse.doOnNext(clientHttpResponse -> consumer.accept(clientHttpResponse.getStatusCode()));
			}

			@Override
			public <T> T extract(ResponseExtractor<T> extractor) {
				return extractor.extract(clientResponse, webClientConfig);
			}
		};
	}

	protected class DefaultWebClientConfig implements WebClientConfig {

		private List<HttpMessageConverter<?>> messageConverters;

		private ResponseErrorHandler responseErrorHandler;

		@Override
		public List<HttpMessageConverter<?>> getMessageConverters() {
			return messageConverters;
		}

		public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
			this.messageConverters = messageConverters;
		}

		@Override
		public ResponseErrorHandler getResponseErrorHandler() {
			return responseErrorHandler;
		}

		public void setResponseErrorHandler(ResponseErrorHandler responseErrorHandler) {
			this.responseErrorHandler = responseErrorHandler;
		}
	}

	protected class DefaultRequestCallback implements Function<ClientHttpRequest, Mono<Void>> {

		private final ClientWebRequest clientWebRequest;

		public DefaultRequestCallback(ClientWebRequest clientWebRequest) {
			this.clientWebRequest = clientWebRequest;
		}

		@Override
		public Mono<Void> apply(ClientHttpRequest clientHttpRequest) {
			clientHttpRequest.getHeaders().putAll(this.clientWebRequest.getHttpHeaders());
			if (clientHttpRequest.getHeaders().getAccept().isEmpty()) {
				clientHttpRequest.getHeaders().setAccept(
						Collections.singletonList(MediaType.ALL));
			}
			clientWebRequest.getCookies().values()
					.stream().flatMap(cookies -> cookies.stream())
					.forEach(cookie -> clientHttpRequest.getCookies().add(cookie.getName(), cookie));
			if (this.clientWebRequest.getBody() != null) {
				return writeRequestBody(this.clientWebRequest.getBody(),
						this.clientWebRequest.getElementType(),
						clientHttpRequest, webClientConfig.getMessageConverters());
			}
			else {
				return clientHttpRequest.setComplete();
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		protected Mono<Void> writeRequestBody(Publisher<?> content,
				ResolvableType requestType, ClientHttpRequest request,
				List<HttpMessageConverter<?>> messageConverters) {

			MediaType contentType = request.getHeaders().getContentType();
			Optional<HttpMessageConverter<?>> converter = resolveConverter(messageConverters, requestType, contentType);
			if (!converter.isPresent()) {
				return Mono.error(new IllegalStateException(
						"Could not encode request body of type '" + contentType
								+ "' with target type '" + requestType.toString() + "'"));
			}
			return converter.get().write((Publisher) content, requestType, contentType, request);
		}

		protected Optional<HttpMessageConverter<?>> resolveConverter(
				List<HttpMessageConverter<?>> messageConverters, ResolvableType type,
				MediaType mediaType) {
			return messageConverters.stream().filter(e -> e.canWrite(type, mediaType)).findFirst();
		}
	}

}