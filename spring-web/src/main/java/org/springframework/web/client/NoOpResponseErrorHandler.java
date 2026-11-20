/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.client;

import java.io.IOException;
import java.util.function.Predicate;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClient.ResponseSpec;
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler;

/**
 * A basic, no operation {@link ResponseErrorHandler} implementation suitable
 * for ignoring any error using the {@link RestTemplate} or {@link RestClient}.
 * <p>This implementation is not suitable with the {@link RestClient} as it uses
 * a list of candidates where the first matching is invoked. If you want to
 * disable default status handlers with the {@code RestClient}, consider
 * registering a noop {@link ResponseSpec.ErrorHandler ErrorHandler} with a
 * predicate that matches all status code, see
 * {@link RestClient.Builder#defaultStatusHandler(Predicate, ErrorHandler)}.
 *
 * @author Stephane Nicoll
 * @since 6.1.7
 * @deprecated as of 7.1 in favor of {@link RestClient.ResponseSpec.ErrorHandler}
 */
@Deprecated(since = "7.1", forRemoval = true)
@SuppressWarnings("removal")
public final class NoOpResponseErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return false;
	}

}
