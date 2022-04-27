/*
 * Copyright 2002-2021 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.util.Assert;

/**
 * Static factory methods to create an {@link ExchangeFunction}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class ExchangeFunctions {

	private static final Log logger = LogFactory.getLog(ExchangeFunctions.class);


	/**
	 * Create an {@code ExchangeFunction} with the given {@code ClientHttpConnector}.
	 * This is the same as calling
	 * {@link #create(ClientHttpConnector, ExchangeStrategies)} and passing
	 * {@link ExchangeStrategies#withDefaults()}.
	 * @param connector the connector to use for connecting to servers
	 * @return the created {@code ExchangeFunction}
	 */
	public static ExchangeFunction create(ClientHttpConnector connector) {
		return create(connector, ExchangeStrategies.withDefaults());
	}

	/**
	 * Create an {@code ExchangeFunction} with the given
	 * {@code ClientHttpConnector} and {@code ExchangeStrategies}.
	 * @param connector the connector to use for connecting to servers
	 * @param strategies the {@code ExchangeStrategies} to use
	 * @return the created {@code ExchangeFunction}
	 */
	public static ExchangeFunction create(ClientHttpConnector connector, ExchangeStrategies strategies) {
		return new DefaultExchangeFunction(connector, strategies);
	}


	private static class DefaultExchangeFunction implements ExchangeFunction {

		private final ClientHttpConnector connector;

		private final ExchangeStrategies strategies;

		private boolean enableLoggingRequestDetails;


		public DefaultExchangeFunction(ClientHttpConnector connector, ExchangeStrategies strategies) {
			Assert.notNull(connector, "ClientHttpConnector must not be null");
			Assert.notNull(strategies, "ExchangeStrategies must not be null");
			this.connector = connector;
			this.strategies = strategies;

			strategies.messageWriters().stream()
					.filter(LoggingCodecSupport.class::isInstance)
					.forEach(reader -> {
						if (((LoggingCodecSupport) reader).isEnableLoggingRequestDetails()) {
							this.enableLoggingRequestDetails = true;
						}
					});
		}


		@Override
		public Mono<ClientResponse> exchange(ClientRequest clientRequest) {
			Assert.notNull(clientRequest, "ClientRequest must not be null");
			HttpMethod httpMethod = clientRequest.method();
			URI url = clientRequest.url();

			return this.connector
					.connect(httpMethod, url, httpRequest -> clientRequest.writeTo(httpRequest, this.strategies))
					.doOnRequest(n -> logRequest(clientRequest))
					.doOnCancel(() -> logger.debug(clientRequest.logPrefix() + "Cancel signal (to close connection)"))
					.onErrorResume(WebClientUtils.WRAP_EXCEPTION_PREDICATE, t -> wrapException(t, clientRequest))
					.map(httpResponse -> {
						String logPrefix = getLogPrefix(clientRequest, httpResponse);
						logResponse(httpResponse, logPrefix);
						return new DefaultClientResponse(
								httpResponse, this.strategies, logPrefix, httpMethod.name() + " " + url,
								() -> createRequest(clientRequest));
					});
		}

		private void logRequest(ClientRequest request) {
			LogFormatUtils.traceDebug(logger, traceOn ->
					request.logPrefix() + "HTTP " + request.method() + " " + request.url() +
							(traceOn ? ", headers=" + formatHeaders(request.headers()) : "")
			);
		}

		private String getLogPrefix(ClientRequest request, ClientHttpResponse response) {
			return request.logPrefix() + "[" + response.getId() + "] ";
		}

		private void logResponse(ClientHttpResponse response, String logPrefix) {
			LogFormatUtils.traceDebug(logger, traceOn -> {
				int code = response.getRawStatusCode();
				HttpStatus status = HttpStatus.resolve(code);
				return logPrefix + "Response " + (status != null ? status : code) +
						(traceOn ? ", headers=" + formatHeaders(response.getHeaders()) : "");
			});
		}

		private String formatHeaders(HttpHeaders headers) {
			return this.enableLoggingRequestDetails ? headers.toString() : headers.isEmpty() ? "{}" : "{masked}";
		}

		private <T> Mono<T> wrapException(Throwable t, ClientRequest r) {
			return Mono.error(() -> new WebClientRequestException(t, r.method(), r.url(), r.headers()));
		}

		private HttpRequest createRequest(ClientRequest request) {
			return new HttpRequest() {

				@Override
				public HttpMethod getMethod() {
					return request.method();
				}

				@Override
				public String getMethodValue() {
					return request.method().name();
				}

				@Override
				public URI getURI() {
					return request.url();
				}

				@Override
				public HttpHeaders getHeaders() {
					return request.headers();
				}
			};
		}
	}

}
