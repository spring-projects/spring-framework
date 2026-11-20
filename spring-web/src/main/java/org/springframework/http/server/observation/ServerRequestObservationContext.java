/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server.observation;

import java.util.Collections;

import io.micrometer.observation.transport.Propagator;
import io.micrometer.observation.transport.RequestReplyReceiverContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

/**
 * Context that holds information for metadata collection regarding
 * {@link ServerHttpObservationDocumentation#HTTP_SERVLET_SERVER_REQUESTS Servlet HTTP requests} observations.
 * <p>This context also extends {@link RequestReplyReceiverContext} for propagating
 * tracing information during HTTP request processing.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public class ServerRequestObservationContext extends RequestReplyReceiverContext<HttpServletRequest, HttpServletResponse> {

	private static final HeaderGetter GETTER = new HeaderGetter();

	private @Nullable String pathPattern;

	public ServerRequestObservationContext(HttpServletRequest request, HttpServletResponse response) {
		super(GETTER);
		setCarrier(request);
		setResponse(response);
	}

	public @Nullable String getPathPattern() {
		return this.pathPattern;
	}

	public void setPathPattern(@Nullable String pathPattern) {
		this.pathPattern = pathPattern;
	}

	static final class HeaderGetter implements Propagator.Getter<HttpServletRequest> {
		@Override
		public String get(HttpServletRequest carrier, String key) {
			return carrier.getHeader(key);
		}

		@Override
		public Iterable<String> getAll(HttpServletRequest carrier, String key) {
			return Collections.list(carrier.getHeaders(key));
		}
	}

}
