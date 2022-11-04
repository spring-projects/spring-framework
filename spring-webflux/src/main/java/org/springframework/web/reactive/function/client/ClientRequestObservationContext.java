/*
 * Copyright 2002-2022 the original author or authors.
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

import io.micrometer.observation.transport.RequestReplySenderContext;

import org.springframework.lang.Nullable;

/**
 * Context that holds information for metadata collection
 * during the {@link ClientHttpObservationDocumentation#HTTP_REACTIVE_CLIENT_EXCHANGES HTTP client exchange observations}.
 * <p>The {@link #getCarrier() tracing context carrier} is a {@link ClientRequest.Builder request builder},
 * since the actual request is immutable. For {@code KeyValue} extraction, the {@link #getRequest() actual request}
 * should be used instead.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class ClientRequestObservationContext extends RequestReplySenderContext<ClientRequest.Builder, ClientResponse> {

	@Nullable
	private String uriTemplate;

	private boolean aborted;

	@Nullable
	private ClientRequest request;


	public ClientRequestObservationContext() {
		super(ClientRequestObservationContext::setRequestHeader);
	}

	private static void setRequestHeader(@Nullable ClientRequest.Builder request, String name, String value) {
		if (request != null) {
			request.headers(headers -> headers.set(name, value));
		}
	}

	/**
	 * Return the URI template used for the current client exchange, {@code null} if none was used.
	 */
	@Nullable
	public String getUriTemplate() {
		return this.uriTemplate;
	}

	/**
	 * Set the URI template used for the current client exchange.
	 */
	public void setUriTemplate(@Nullable String uriTemplate) {
		this.uriTemplate = uriTemplate;
	}

	/**
	 * Whether the client aborted the current HTTP exchange before receiving any response.
	 * @return whether the exchange has been aborted
	 */
	public boolean isAborted() {
		return this.aborted;
	}

	/**
	 * Set whether the client aborted the current HTTP exchange.
	 * @param aborted whether the exchange has been aborted
	 */
	void setAborted(boolean aborted) {
		this.aborted = aborted;
	}

	/**
	 * Return the immutable client request.
	 */
	@Nullable
	public ClientRequest getRequest() {
		return this.request;
	}

	/**
	 * Set the client request.
	 */
	public void setRequest(ClientRequest request) {
		this.request = request;
	}
}
