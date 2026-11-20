/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.client.observation;

import io.micrometer.observation.transport.RequestReplySenderContext;
import org.jspecify.annotations.Nullable;

import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Context that holds information for metadata collection during the
 * {@link ClientHttpObservationDocumentation#HTTP_CLIENT_EXCHANGES client HTTP exchanges} observations.
 *
 * <p>This context also extends {@link RequestReplySenderContext} for propagating tracing
 * information with the HTTP client exchange.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class ClientRequestObservationContext extends RequestReplySenderContext<ClientHttpRequest, ClientHttpResponse> {

	private @Nullable String uriTemplate;


	/**
	 * Create an observation context for {@link ClientHttpRequest} observations.
	 * @param request the HTTP client request
	 */
	public ClientRequestObservationContext(ClientHttpRequest request) {
		super(ClientRequestObservationContext::setRequestHeader);
		setCarrier(request);
	}

	private static void setRequestHeader(@Nullable ClientHttpRequest request, String name, String value) {
		if (request != null) {
			request.getHeaders().set(name, value);
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

}
