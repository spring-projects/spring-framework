/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.client;

import io.micrometer.observation.transport.RequestReplySenderContext;
import org.jspecify.annotations.Nullable;

/**
 * Context that holds information for metadata collection during the
 * {@link ClientHttpObservationDocumentation#HTTP_REACTIVE_CLIENT_EXCHANGES HTTP client exchange observations}.
 *
 * <p>The {@link #getCarrier() tracing context carrier} is a {@link ClientRequest.Builder request builder},
 * since the actual request is immutable. For {@code KeyValue} extraction,
 * the {@link #getRequest() actual request} should be used instead.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class ClientRequestObservationContext extends RequestReplySenderContext<ClientRequest.Builder, ClientResponse> {

	private @Nullable String uriTemplate;

	private boolean aborted;

	private @Nullable ClientRequest request;


	/**
	 * Create a new Observation context for HTTP client observations.
	 * @param request client request builder
	 * @since 6.1.2
	 */
	public ClientRequestObservationContext(ClientRequest.Builder request) {
		super(ClientRequestObservationContext::setRequestHeader);
		setCarrier(request);
		setRequest(request.build());
	}


	private static void setRequestHeader(ClientRequest.@Nullable Builder request, String name, String value) {
		if (request != null) {
			request.headers(headers -> headers.set(name, value));
		}
	}


	/**
	 * Set the URI template used for the current client exchange.
	 */
	public void setUriTemplate(@Nullable String uriTemplate) {
		this.uriTemplate = uriTemplate;
	}

	/**
	 * Return the URI template used for the current client exchange, {@code null} if none was used.
	 */
	public @Nullable String getUriTemplate() {
		return this.uriTemplate;
	}

	/**
	 * Set whether the client aborted the current HTTP exchange.
	 * @param aborted whether the exchange has been aborted
	 */
	void setAborted(boolean aborted) {
		this.aborted = aborted;
	}

	/**
	 * Whether the client aborted the current HTTP exchange before receiving any response.
	 * @return whether the exchange has been aborted
	 */
	public boolean isAborted() {
		return this.aborted;
	}

	/**
	 * Set the client request.
	 */
	public void setRequest(ClientRequest request) {
		this.request = request;
	}

	/**
	 * Return the immutable client request.
	 */
	public @Nullable ClientRequest getRequest() {
		return this.request;
	}

}
